package eungju.rxcurrencyconverter

import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

interface Fixer {
    @GET("latest")
    fun latest(@Query("base") base: String? = null): Observable<CurrencyRates>
}