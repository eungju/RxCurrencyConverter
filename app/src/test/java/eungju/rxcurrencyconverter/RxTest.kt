package eungju.rxcurrencyconverter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.Test

class RxTest {
    @Test
    fun scan() {
        val observer = TestObserver.create<Int>()
        Observable.range(0, 3).scan(0, { current, e -> current + e }).subscribe(observer)
        observer.assertValues(0, 0, 1, 3)
    }
}