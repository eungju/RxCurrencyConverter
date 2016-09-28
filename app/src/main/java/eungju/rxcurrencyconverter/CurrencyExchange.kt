package eungju.rxcurrencyconverter

data class CurrencyExchange(val base: String, val date: String, val rates: Map<String, Double>)