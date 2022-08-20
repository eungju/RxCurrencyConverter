package eungju.rxcurrencyconverter

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface FreeApi {
    @GET("convert")
    fun convert(@Query("q") q: String): Single<ConvertResponse>

    data class ConvertResponse(val results: Map<String, Result>) {
        data class Result(val id: String, val `val`: Double, val to: String, val fr: String)
    }

    @GET("currencies")
    fun currencies(): Single<CurrenciesResponse>

    data class CurrenciesResponse(val results: Map<String, Result>) {
        data class Result(val id: String, val currencyName: String, val currencySymbol: String)
    }
}
