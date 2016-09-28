package eungju.rxcurrencyconverter

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface Fixer {
    @GET("latest")
    fun latest(@Query("base") base: String? = null): Observable<CurrencyExchange>
}