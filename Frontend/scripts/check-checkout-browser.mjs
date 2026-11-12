// Destructive browser QA: use ONLY the isolated frontend on 5174 backed by a disposable database.
// Start Chrome with --headless=new --remote-debugging-port=9229 and a separate user-data-dir.
import assert from 'node:assert/strict';
import { writeFile } from 'node:fs/promises';

const pages = await (await fetch('http://127.0.0.1:9229/json')).json();
const page = pages.find(page => page.type === 'page');
if (!page) throw new Error('Isolated headless Chrome is not running.');
const socket = new WebSocket(page.webSocketDebuggerUrl);
await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
let id = 0;
const pending = new Map();
const exceptions = [];
const checkoutKeys = [];
let dropFirstCheckoutResponse = false;
socket.onmessage = event => {
    const message = JSON.parse(event.data);
    if (message.id) {
        const callback = pending.get(message.id);
        pending.delete(message.id);
        if (message.error) callback?.reject(new Error(message.error.message));
        else callback?.resolve(message.result);
    }
    if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.text);
    if (message.method === 'Page.javascriptDialogOpening') command('Page.handleJavaScriptDialog', { accept: true }).catch(() => {});
    if (message.method === 'Network.requestWillBeSent' && message.params.request.url.endsWith('/api/order/checkout')) {
        const headers = message.params.request.headers;
        checkoutKeys.push(headers['Idempotency-Key'] || headers['idempotency-key']);
    }
    if (message.method === 'Fetch.requestPaused') {
        const { requestId, responseStatusCode } = message.params;
        if (dropFirstCheckoutResponse && responseStatusCode === 201) {
            dropFirstCheckoutResponse = false;
            command('Fetch.failRequest', { requestId, errorReason: 'ConnectionReset' }).catch(() => {});
        } else command('Fetch.continueRequest', { requestId }).catch(() => {});
    }
};
function command(method, params = {}) {
    return new Promise((resolve, reject) => {
        const requestId = ++id;
        const timer = setTimeout(() => { pending.delete(requestId); reject(new Error('CDP timeout: ' + method)); }, 25000);
        pending.set(requestId, { resolve: value => { clearTimeout(timer); resolve(value); }, reject: error => { clearTimeout(timer); reject(error); } });
        socket.send(JSON.stringify({ id: requestId, method, params }));
    });
}
async function evaluate(expression) {
    const result = await command('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text);
    return result.result.value;
}
async function waitFor(expression) {
    const until = Date.now() + 25000;
    while (Date.now() < until) {
        if (await evaluate(expression)) return;
        await new Promise(resolve => setTimeout(resolve, 100));
    }
    throw new Error('Browser condition timed out: ' + expression + '\n' + await evaluate('document.querySelector(".modal")?.innerText'));
}
async function fill(name, value) {
    await evaluate(`(() => {
        const input = document.querySelector('input[name="${name}"]');
        if (!input) throw new Error('Missing input: ${name}');
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, ${JSON.stringify(value)});
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
    })()`);
}
async function click(selector) { await evaluate(`document.querySelector(${JSON.stringify(selector)}).click()`); }

try {
    await command('Runtime.enable');
    await command('Page.enable');
    await command('Network.enable');
    await command('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
    await command('Page.navigate', { url: 'http://127.0.0.1:5174' });
    await waitFor('document.querySelectorAll(".product-card").length > 0');
    await waitFor('document.querySelectorAll("#seller-filter option").length > 1');
    await evaluate(`(() => {
        const seller = document.querySelector('#seller-filter');
        const option = Array.from(seller.options).find(option => option.text.startsWith('filter-') && !option.text.endsWith(' other'));
        if (!option) throw new Error('Run ProductFilterDatabaseTests to create isolated filter fixtures.');
        seller.value = option.value; seller.dispatchEvent(new Event('change', { bubbles: true }));
    })()`);
    await evaluate(`(() => {
        const sellerName = document.querySelector('#seller-filter').selectedOptions[0].text;
        const category = document.querySelector('.advanced-filters label:nth-child(2) select');
        category.value = Array.from(category.options).find(option => option.text === sellerName).value;
        category.dispatchEvent(new Event('change', { bubbles: true }));
    })()`);
    await evaluate(`(() => {
        for (const [index, value] of [[3, '90'], [4, '200']]) {
            const input = document.querySelector('.advanced-filters label:nth-child(' + index + ') input');
            Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, value);
            input.dispatchEvent(new Event('input', { bubbles: true }));
        }
    })()`);
    await click('.filter-actions .primary');
    await waitFor('document.querySelectorAll(".product-card").length === 2 && !document.querySelector(".skeleton")');
    console.log('PASS: seller, category and price filters combine in the live product feed.');
    await click('.filter-actions .text-button');
    await waitFor('document.querySelectorAll(".product-card").length > 2');
    await click('.history-link');
    await waitFor('!!document.querySelector(".auth-tabs")');
    await click('.modal .close');
    console.log('PASS: order history requires signing in.');
    await evaluate(`Array.from(document.querySelectorAll('.product-bottom .quantity-controls button:last-child')).find(button => !button.disabled).click()`);
    await waitFor('Array.from(document.querySelectorAll(".card-quantity output")).some(output => output.textContent === "1")');
    console.log('PASS: guests see goods and visible cart quantities.');
    await click('.basket-button');
    await click('.checkout-button');
    await waitFor('!!document.querySelector(".auth-tabs")');
    await click('.auth-tabs button:last-child');
    for (const [key, value] of Object.entries({ name: '123!', phone: 'gibberish', email: 'invalid', password: 'short', confirmPassword: 'different' })) await fill(key, value);
    await click('.login-form .primary');
    await waitFor('!!document.querySelector(".validation-summary")');
    assert.match(await evaluate('document.querySelector(".validation-summary").textContent'), /phone/);
    console.log('PASS: signup rejects malformed account fields.');
    const email = 'checkout-browser-' + Date.now() + '@example.test';
    const password = 'Browser-test-password-123';
    for (const [key, value] of Object.entries({ name: 'Browser Test', phone: '+998 90 123 45 67', email, password, confirmPassword: password })) await fill(key, value);
    await click('.login-form .primary');
    await waitFor('!!document.querySelector(".checkout-modal")');
    assert.ok(await evaluate('document.querySelector(".checkout-summary").textContent.includes("× 1")'));
    console.log('PASS: signup signs in and preserves the guest basket at checkout.');
    await fill('phone', 'not a phone');
    await click('.delivery-form .primary');
    await waitFor('!!document.querySelector("#error-phone")');
    console.log('PASS: checkout shows field-specific delivery errors.');
    for (const [key, value] of Object.entries({ recipientName: 'Browser Test', phone: '+998 90 123 45 67', addressLine1: '12 Test Street', city: 'Tashkent', postalCode: '100000', country: 'Uzbekistan' })) await fill(key, value);
    await click('.checkout-consent input');
    await command('Fetch.enable', { patterns: [{ urlPattern: '*/api/order/checkout', requestStage: 'Response' }] });
    dropFirstCheckoutResponse = true;
    await click('.delivery-form .primary');
    await waitFor('!!document.querySelector(".retry-note") && !!document.querySelector(".delivery-form .form-error")');
    const savedKey = await evaluate('JSON.parse(sessionStorage.getItem("animerch.pending-checkout.v1")).key');
    assert.ok(savedKey);
    assert.ok(await evaluate('document.querySelector(".delivery-form fieldset").disabled'));
    console.log('PASS: a lost success response keeps the basket and delivery details locked for retry.');
    await command('Fetch.disable');
    await command('Page.navigate', { url: 'http://127.0.0.1:5174' });
    await waitFor('document.querySelector(".basket-button")?.textContent.includes("Resume order")');
    await click('.basket-button');
    await waitFor('!!document.querySelector(".checkout-modal")');
    await click('.delivery-form .primary');
    await waitFor('!!document.querySelector(".auth-tabs")');
    await fill('email', email);
    await fill('password', password);
    await click('.login-form .primary');
    await waitFor('!!document.querySelector(".checkout-modal")');
    await click('.delivery-form .primary');
    await waitFor('!!document.querySelector(".order-success")');
    assert.equal(checkoutKeys.length, 2);
    assert.equal(checkoutKeys[0], savedKey);
    assert.equal(checkoutKeys[1], savedKey);
    const orderCount = await evaluate(`(async () => {
        const login = await (await fetch('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: ${JSON.stringify(email)}, password: ${JSON.stringify(password)} }) })).json();
        const orders = await (await fetch('/api/order/my', { headers: { Authorization: 'Bearer ' + login.accessToken } })).json();
        return orders.totalElements;
    })()`);
    assert.equal(orderCount, 1);
    assert.equal(await evaluate('sessionStorage.getItem("animerch.pending-checkout.v1")'), null);
    assert.equal(exceptions.length, 0, exceptions.join('\n'));
    console.log('PASS: reload and reauthentication preserve the key, return one confirmed order, and clear pending checkout.');
    const desktop = await command('Page.captureScreenshot', { format: 'png' });
    await writeFile('../target/checkout-confirmation.png', Buffer.from(desktop.data, 'base64'));
    await command('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
    assert.ok(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'));
    const mobile = await command('Page.captureScreenshot', { format: 'png' });
    await writeFile('../target/checkout-mobile.png', Buffer.from(mobile.data, 'base64'));
    console.log('PASS: mobile viewport has no horizontal overflow. Screenshots saved in target/.');
    await click('.receipt-history');
    await waitFor('document.querySelectorAll(".order-card").length === 1');
    assert.match(await evaluate('document.querySelector(".order-card").textContent'), /placed/);
    await click('.order-details-toggle');
    assert.match(await evaluate('document.querySelector(".order-details").textContent'), /12 Test Street/);
    assert.match(await evaluate('document.querySelector(".order-details").textContent'), /Cancelled items/);
    assert.ok(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'));
    console.log('PASS: the confirmed purchase appears in My orders with statuses, delivery details and totals.');
} finally { socket.close(); }
