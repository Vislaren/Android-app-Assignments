fun processList(
    numbers: List<Int>,
    predicate: (Int) -> Boolean
): List<Int> {
    return numbers.filter { predicate(it) }
}

fun main() {
    val nums = listOf(1, 2, 3, 4, 5, 6)

    // Test 1: Filter even numbers
    // { it % 2 == 0 } means: keep only numbers with no remainder when divided by 2
    val even = processList(nums) { it % 2 == 0 }
    println("Even numbers: $even") 

    // Test 2: Filter odd numbers
    val odd = processList(nums) { it % 2 != 0 }
    println("Odd numbers: $odd") 

    // Test 3: Filter numbers greater than 3
    val greaterThanThree = processList(nums) { it > 3 }
    println("Numbers greater than 3: $greaterThanThree")
}