# Exercise 1 – Build Your Own Higher-Order Function

**Course:** SE 3242 – Android Application Development  
**Week:** 2 | **Topic:** Lambdas & Higher-Order Functions  
**Students:**  Fanyi Charllson & Adrien Tello

---

## Objective

Implement a higher-order function called `processList` that accepts a list
of integers and a lambda predicate, returning only elements that satisfy
the condition.

---

## Concepts Demonstrated

**Higher-Order Functions**  
A function that takes another function as a parameter. Here, `processList`
takes `predicate` as a parameter — and that parameter is itself a lambda function.

**Lambda / Predicate**  
The `predicate` has type `(Int) -> Boolean` — it takes one Int and returns
true (keep) or false (discard).

**The `filter` Function**  
Internally uses Kotlin's built-in `filter`, which keeps only elements where
the lambda returns true.

**The `it` Keyword**  
When a lambda has one parameter, Kotlin auto-names it `it`.  
So `{ it % 2 == 0 }` means: "is the current element even?"

---

## How It Works — Step by Step
```kotlin
val nums = listOf(1, 2, 3, 4, 5, 6)
val even = processList(nums) { it % 2 == 0 }
```

| Element | it % 2 == 0 | Result    |
|---------|-------------|-----------|
| 1       | false       | Discarded |
| 2       | true        | Kept      |
| 3       | false       | Discarded |
| 4       | true        | Kept      |
| 5       | false       | Discarded |
| 6       | true        | Kept      |

**Output:** `[2, 4, 6]`

---

## Test Cases
```kotlin
processList(nums) { it % 2 == 0 }  // Even numbers   -> [2, 4, 6]
processList(nums) { it % 2 != 0 }  // Odd numbers    -> [1, 3, 5]
processList(nums) { it > 3 }       // Greater than 3 -> [4, 5, 6]
```

---

## Key Takeaway

> `processList` is a manual implementation of Kotlin's built-in `filter`.  
> Building it from scratch shows understanding of higher-order functions and lambdas.