package playground

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Good explanation on backpressure Rx2:
 * https://stackoverflow.com/questions/40323307/observable-vs-flowable-rxjava2
 */

class BackpressureExercises {

    val dishesRange = Observable.range(1, 1000000000).map { Dish(it) }
    val dishesInterval = Observable.interval(1, TimeUnit.MILLISECONDS).map { Dish(it.toInt()) }
    val dishesZip: Observable<Dish> = Observable.zip(Observable.interval(1, TimeUnit.MILLISECONDS),
            Observable.interval(10, TimeUnit.SECONDS),
            BiFunction { t1, t2 -> Dish(t1.toInt() + t2.toInt()) })

}

data class Dish(private val id: Int, private val oneKb: ByteArray = ByteArray(1024)) {
    init {
        println("Created: $id")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dish

        if (id != other.id) return false
        if (!Arrays.equals(oneKb, other.oneKb)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + Arrays.hashCode(oneKb)
        return result
    }
}