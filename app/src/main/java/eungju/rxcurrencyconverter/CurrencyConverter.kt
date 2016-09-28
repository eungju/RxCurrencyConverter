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

class CurrencyConverter @Inject constructor(fixer: Fixer) {
    val refresh: PublishRelay<Unit> = PublishRelay.create()
    val refreshing: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)
    val fromCurrencySet: BehaviorRelay<Currency> = BehaviorRelay.createDefault(Currency.getInstance("USD"))
    val fromCurrencyUpdate: PublishRelay<Currency> = PublishRelay.create()
    val fromAmountSet: BehaviorRelay<BigDecimal> = BehaviorRelay.createDefault(BigDecimal.ONE)
    val fromAmountUpdate: PublishRelay<BigDecimal> = PublishRelay.create()
    val toCurrencySet: BehaviorRelay<Currency> = BehaviorRelay.createDefault(Currency.getInstance("KRW"))
    val toCurrencyUpdate: PublishRelay<Currency> = PublishRelay.create()
    val toAmountSet: BehaviorRelay<BigDecimal> = BehaviorRelay.create()
    val toAmountUpdate: PublishRelay<BigDecimal> = PublishRelay.create()

    val fromCurrency: Observable<Currency> = Observable.merge(fromCurrencySet, fromCurrencyUpdate)
    val toCurrency: Observable<Currency> = Observable.merge(toCurrencySet, toCurrencyUpdate)
    private val currencyExchange: Observable<CurrencyExchange> = fromCurrency.distinctUntilChanged()
            .concatMap { fromCurrency ->
                fixer.latest(fromCurrency.currencyCode)
                        .map { it.copy(rates = it.rates + Pair(fromCurrency.currencyCode, 1.0))}
                        .doOnError { refreshing.accept(false) }
                        .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { refreshing.accept(false) }
            .share()
    val date: Observable<String> = currencyExchange.map { it.date }
    val currencies: Observable<List<Currency>> = currencyExchange
            .map { it.rates.keys.sorted().map { Currency.getInstance(it) } }
    val fromAmount: Observable<BigDecimal> = Observable.merge(fromAmountSet, fromAmountUpdate)
            .mergeWith(Observable.merge(toAmountSet, toAmountUpdate).withLatestFrom(currencyExchange.withLatestFrom(fromCurrency, BiFunction<CurrencyExchange, Currency, Pair<Currency, CurrencyExchange>> { a, b -> Pair(b, a) }), toCurrency, Function3 { amount, fromAndExchange, to ->
                val (from, exchange) = fromAndExchange
                if (from.currencyCode != exchange.base) {
                    throw IllegalStateException(String.format("FROM: %s != %s", from.currencyCode, exchange.base))
                }
                amount.divide(BigDecimal(exchange.rates[to.currencyCode] as Double), from.defaultFractionDigits, RoundingMode.FLOOR)
            }))
    val toAmount: Observable<BigDecimal> = Observable.merge(toAmountSet, toAmountUpdate)
            .mergeWith(Observable.combineLatest(Observable.merge(fromAmountSet, fromAmountUpdate), currencyExchange.withLatestFrom(fromCurrency, BiFunction<CurrencyExchange, Currency, Pair<Currency, CurrencyExchange>> { a, b -> Pair(b, a) }), toCurrency, Function3 { amount, fromAndExchange, to ->
                val (from, exchange) = fromAndExchange
                if (from.currencyCode != exchange.base) {
                    throw IllegalStateException(String.format("TO: %s != %s", from.currencyCode, exchange.base))
                }
                amount.multiply(BigDecimal(exchange.rates[to.currencyCode] as Double)).setScale(to.defaultFractionDigits, RoundingMode.FLOOR)
            }))
}