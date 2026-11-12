package com.shop.animerch.validation;

public final class InputRules {
    private InputRules() {}

    public static final String NAME = "[\\p{L}\\p{M}][\\p{L}\\p{M} .'’-]*";
    public static final String PHONE = "(?=(?:[^0-9]*[0-9]){7,15}[^0-9]*$)\\+?[1-9][0-9 ()-]*";
    public static final String ADDRESS = "(?=.*[\\p{L}\\p{N}])[^\\p{Cntrl}]+";
    public static final String POSTAL_CODE = "[\\p{L}\\p{N}][\\p{L}\\p{N} -]*";
}
