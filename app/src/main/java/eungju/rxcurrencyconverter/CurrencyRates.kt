package eungju.rxcurrencyconverter

data class CurrencyRates(val base: String, val date: String, val rates: Map<String, Double>)