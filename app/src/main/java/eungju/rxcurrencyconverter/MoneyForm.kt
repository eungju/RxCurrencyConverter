package eungju.rxcurrencyconverter

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.math.BigDecimal
import java.util.Currency

class MoneyForm {
    val currencies: BehaviorRelay<List<Currency>> = BehaviorRelay.create()
    val currencySet: BehaviorRelay<Currency> = BehaviorRelay.create()
    val currencySelect: PublishRelay<Int> = PublishRelay.create()
    val amountSet: BehaviorRelay<BigDecimal> = BehaviorRelay.create()
    val keyPress: PublishRelay<String> = PublishRelay.create()

    val currency: Observable<Currency> = currencySet
            .mergeWith(Observable.combineLatest(currencySelect, currencies, BiFunction { select, currencies -> currencies[select] }))
            .cacheWithInitialCapacity(1)
    val currencySetIndex: Observable<Int> = Observable.combineLatest<Currency, List<Currency>, Int>(currencySet, currencies, BiFunction { currency, currencies ->
                currencies.indexOf(currency)
            })
            .cacheWithInitialCapacity(1)
    val amount: Observable<BigDecimal> = amountSet.map { amount -> { current: BigDecimal -> amount } }
            .mergeWith(keyPress.map { keyPress ->
                when (keyPress) {
                    "delete" -> { current: BigDecimal -> current.divideToIntegralValue(BigDecimal.TEN) }
                    "clear" -> { current: BigDecimal -> BigDecimal.ZERO }
                    else -> { current: BigDecimal ->
                        current
                                .divideToIntegralValue(BigDecimal.ONE)
                                .multiply(BigDecimal.TEN)
                                .plus(BigDecimal(Integer.parseInt(keyPress).toDouble()))
                    }
                }
            })
            .scan(BigDecimal.ZERO, { current, f -> f(current) })
            .skip(1)
            .cacheWithInitialCapacity(1)
    val currencyUpdate: Observable<Currency> = currencySelect
            .withLatestFrom(currency, BiFunction { currencySelect, currency -> currency })
    val amountUpdate: Observable<BigDecimal> = keyPress
            .withLatestFrom(amount, BiFunction { keyPress, amount -> amount })
}