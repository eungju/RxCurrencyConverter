package eungju.rxcurrencyconverter

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxAdapterView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import kotlinx.android.synthetic.main.view_money_form.view.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Currency

class MoneyFormView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private lateinit var presenter: MoneyForm

    init {
        View.inflate(context, R.layout.view_money_form, this)
        if (!isInEditMode) {
            presenter = MoneyForm()
        }
    }

    private lateinit var subscriptions: CompositeDisposable

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscriptions = CompositeDisposable()
        if (!isInEditMode) {
            //output
            subscriptions.add(presenter.currencies
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        currency.adapter = ArrayAdapter<String>(context, R.layout.view_currency_item, it.map { it.currencyCode })
                    })
            subscriptions.add(presenter.currencySetIndex
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { currency.setSelection(it) })
            subscriptions.add(Observable
                    .combineLatest<BigDecimal, Currency, String>(presenter.amount, presenter.currency, BiFunction { amount, currency ->
                        val format = DecimalFormat()
                        format.currency = currency
                        format.groupingSize = 3
                        format.minimumFractionDigits = 0
                        format.maximumFractionDigits = currency.defaultFractionDigits
                        format.format(amount)
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(RxTextView.text(amount)))
            //input
            subscriptions.add(RxAdapterView.itemSelections(currency)
                    .skip(1)
                    .subscribe(presenter.currencySelect))
            subscriptions.add(Observable.merge((0..11).map { keys.getChildAt(it) }.map { v -> RxView.clicks(v).map { v.tag as String } })
                    .subscribe(presenter.keyPress))
        }
    }

    override fun onDetachedFromWindow() {
        subscriptions.dispose()
        super.onDetachedFromWindow()
    }

    fun setCurrencies(currencies: List<Currency>) {
        presenter.currencies.accept(currencies)
    }

    fun setCurrency(currency: Currency) {
        presenter.currencySet.accept(currency)
    }

    fun setAmount(amount: BigDecimal) {
        presenter.amountSet.accept(amount)
    }

    fun currency(): Observable<Currency> = presenter.currency

    fun currencyUpdate(): Observable<Currency> = presenter.currencyUpdate

    fun amount(): Observable<BigDecimal> = presenter.amount

    fun amountUpdate(): Observable<BigDecimal> = presenter.amountUpdate
}
