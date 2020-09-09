package ru.skillbranch.kotlinexample.extensions


fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean) : List<T> {
    var yielding = false
    val list = ArrayList<T>()
    for (item in this.asReversed())
        if (yielding)
            list.add(item)
        else if (predicate(item)) {
            yielding = true
        }
    return list.asReversed()
}