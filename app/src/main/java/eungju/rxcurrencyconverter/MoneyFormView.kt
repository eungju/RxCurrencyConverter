package eungju.rxcurrencyconverter

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import com.jakewharton.rxbinding.view.RxView
import com.jakewharton.rxbinding.widget.RxAdapterView
import com.jakewharton.rxbinding.widget.RxTextView
import kotlinx.android.synthetic.main.view_money_form.view.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

class MoneyFormView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private lateinit var presenter: MoneyForm

    init {
        View.inflate(context, R.layout.view_money_form, this)
        if (!isInEditMode) {
            presenter = MoneyForm()
        }
    }

    private lateinit var subscriptions: CompositeSubscription

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscriptions = CompositeSubscription()
        if (!isInEditMode) {
            //input
            subscriptions.add(RxAdapterView.itemSelections(currency).skip(1)
                    .subscribe(presenter.currencySelect))
            subscriptions.add(Observable.merge((0..11).map { keys.getChildAt(it) }.map { v -> RxView.clicks(v).map { v.tag as String } })
                    .subscribe(presenter.keyPress))
            //output
            subscriptions.add(presenter.currencies
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        currency.adapter = ArrayAdapter<String>(context, R.layout.view_currency_item, it.map { it.currencyCode })
                    })
            subscriptions.add(presenter.currencyIndex
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { currency.setSelection(it) })
            subscriptions.add(Observable
                    .combineLatest(presenter.amount, presenter.currency, { amount, currency ->
                        val format = DecimalFormat()
                        format.currency = currency
                        format.groupingSize = 3
                        format.minimumFractionDigits = 0
                        format.maximumFractionDigits = currency.defaultFractionDigits
                        format.format(amount)
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(RxTextView.text(amount)))
        }
    }

    override fun onDetachedFromWindow() {
        subscriptions.unsubscribe()
        super.onDetachedFromWindow()
    }

    fun setCurrencies(currencies: List<Currency>) {
        presenter.currencies.call(currencies)
    }

    fun setCurrency(currency: Currency) {
        presenter.currencySet.call(currency)
    }

    fun setAmount(amount: BigDecimal) {
        presenter.amountSet.call(amount)
    }

    fun currency(): Observable<Currency> = presenter.currency

    fun currencyUpdate(): Observable<Currency> = presenter.currencyUpdate

    fun amount(): Observable<BigDecimal> = presenter.amount

    fun amountUpdate(): Observable<BigDecimal> = presenter.amountUpdate
}
