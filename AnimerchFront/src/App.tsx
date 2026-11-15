import "./App.css"
import Navigation from '../Components/Navigation'
import Main from '../Components/Main'
import Welcome from '../Components/Welcome'
import Banner from '../Components/Banner'
import Footer from '../Components/Footer'
function App() {
  return (
    <div className="App">
      <Navigation />
      <Welcome />
      <div className="banners">
        <Banner />
      </div>
      <Main />
      <Footer />
    </div>
  )
}

export default App
