package eungju.rxcurrencyconverter

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function4
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
    private val currencyRates: Observable<CurrencyRates> = fromCurrency.distinctUntilChanged()
            .mergeWith(refresh.withLatestFrom(fromCurrency, BiFunction { refresh, fromCurrency -> fromCurrency }))
            .doOnNext { refreshing.accept(true) }
            .concatMap { fromCurrency ->
                fixer.latest(fromCurrency.currencyCode)
                        .map { it.copy(rates = it.rates + Pair(fromCurrency.currencyCode, 1.0))}
                        .doOnError { refreshing.accept(false) }
                        .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { refreshing.accept(false) }
            .share()
    val date: Observable<String> = currencyRates.map { it.date }
    val currencies: Observable<List<Currency>> = currencyRates
            .map { it.rates.keys.sorted().map { Currency.getInstance(it) } }
    val fromAmount: Observable<BigDecimal> = Observable.merge(fromAmountSet, fromAmountUpdate)
            .mergeWith(Observable.merge(toAmountSet, toAmountUpdate).withLatestFrom(fromCurrency, toCurrency, currencyRates, Function4 { amount, from, to, rates ->
                amount.divide(BigDecimal(rates.rates[to.currencyCode] as Double), from.defaultFractionDigits, RoundingMode.FLOOR)
            }))
    val toAmount: Observable<BigDecimal> = Observable.merge(toAmountSet, toAmountUpdate)
            .mergeWith(Observable.combineLatest(Observable.merge(fromAmountSet, fromAmountUpdate), fromCurrency, toCurrency, currencyRates, Function4 { amount, from, to, rates ->
                amount.multiply(BigDecimal(rates.rates[to.currencyCode] as Double)).setScale(to.defaultFractionDigits, RoundingMode.FLOOR)
            }))
}