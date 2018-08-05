package eungju.rxcurrencyconverter

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import javax.inject.Inject

class CurrencyConverter @Inject constructor(freeApi: FreeApi) {
    val init: PublishRelay<Unit> = PublishRelay.create()
    val refresh: PublishRelay<Unit> = PublishRelay.create()
    val refreshing: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)
    private val fromCurrencySet: BehaviorRelay<Currency> = BehaviorRelay.createDefault(Currency.getInstance("USD"))
    val fromCurrencyUpdate: PublishRelay<Currency> = PublishRelay.create()
    private val fromAmountSet: BehaviorRelay<BigDecimal> = BehaviorRelay.createDefault(BigDecimal.ONE)
    val fromAmountUpdate: PublishRelay<BigDecimal> = PublishRelay.create()
    private val toCurrencySet: BehaviorRelay<Currency> = BehaviorRelay.createDefault(Currency.getInstance("KRW"))
    val toCurrencyUpdate: PublishRelay<Currency> = PublishRelay.create()
    private val toAmountSet: BehaviorRelay<BigDecimal> = BehaviorRelay.create()
    val toAmountUpdate: PublishRelay<BigDecimal> = PublishRelay.create()

    val fromCurrency: Observable<Currency> = Observable.merge(fromCurrencySet, fromCurrencyUpdate)
    val toCurrency: Observable<Currency> = Observable.merge(toCurrencySet, toCurrencyUpdate)
    private val exchangeRate: Observable<ExchangeRate> = Observable.combineLatest(fromCurrency.distinctUntilChanged(), toCurrency.distinctUntilChanged(), BiFunction {
        from: Currency, to: Currency -> Pair(from, to)
    })
            .concatMap { (from, to) ->
                freeApi.convert(from.currencyCode + "_" + to.currencyCode)
                        .map { it.results.values.first().let { ExchangeRate(Currency.getInstance(it.fr), Currency.getInstance(it.to), it.`val`) } }
                        .doOnError { refreshing.accept(false) }
                        .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { refreshing.accept(false) }
            .share()
    val currencies: Observable<List<Currency>> = Observable.merge(init, refresh)
            .concatMap { _ ->
                freeApi.currencies()
                        .map { it.results.keys.sorted().map(Currency::getInstance) }
                        .doOnError { refreshing.accept(false) }
                        .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { refreshing.accept(false) }
            .share()
    private val fromAmountChange = Observable.merge(fromAmountSet, fromAmountUpdate)
    private val toAmountChange = Observable.merge(toAmountSet, toAmountUpdate)
    val fromAmount: Observable<BigDecimal> = fromAmountChange
            .mergeWith(toAmountChange.withLatestFrom(exchangeRate.withLatestFrom(fromCurrency, BiFunction { a: ExchangeRate, b: Currency -> Pair(b, a) }), toCurrency, Function3 { amount, fromAndExchange, to ->
                val (from, exchange) = fromAndExchange
                if (from != exchange.from) {
                    throw IllegalStateException(String.format("FROM: %s != %s", from.currencyCode, exchange.from))
                }
                amount.divide(BigDecimal(exchange.rate), from.defaultFractionDigits, RoundingMode.FLOOR)
            }))
    val toAmount: Observable<BigDecimal> = toAmountChange
            .mergeWith(Observable.combineLatest(fromAmountChange, exchangeRate.withLatestFrom(fromCurrency, BiFunction { a: ExchangeRate, b: Currency -> Pair(b, a) }), toCurrency, Function3 { amount, fromAndExchange, to ->
                val (from, exchange) = fromAndExchange
                if (from != exchange.from) {
                    throw IllegalStateException(String.format("FROM: %s != %s", from.currencyCode, exchange.from))
                }
                amount.multiply(BigDecimal(exchange.rate)).setScale(to.defaultFractionDigits, RoundingMode.FLOOR)
            }))
}
