# Exercise 2 – Transforming Between Collection Types

**Course:** SE 3242 – Android Application Development  
**Week:** 2 | **Topic:** Collections & Lambdas  
**Students:** Fanyi Charllson & Adrien Tello

---

## Objective

Given a list of strings, create a Map where keys are the strings and values
are their lengths. Then print only entries where the length is greater than 4.

---

## Concepts Demonstrated

**`associateWith`**  
Transforms a List into a Map. Each element becomes a key, and the lambda
result becomes its value.
```kotlin
words.associateWith { it.length }
// "apple" -> 5, "cat" -> 3, "banana" -> 6 ...
```

**Filtering a Map**  
`filter` works on Maps too. `it.key` is the word, `it.value` is the length.

**Method Chaining**  
Operations are chained — each step feeds into the next cleanly.

---

## Step by Step
```kotlin
val words = listOf("apple", "cat", "banana", "dog", "elephant")
```

After `associateWith`:
| Key      | Value (length) |
|----------|----------------|
| apple    | 5              |
| cat      | 3              |
| banana   | 6              |
| dog      | 3              |
| elephant | 8              |

After `filter { it.value > 4 }`:
| Key      | Value |
|----------|-------|
| apple    | 5     |
| banana   | 6     |
| elephant | 8     |

---

## Expected Output
```
apple has length 5
banana has length 6
elephant has length 8
```

---

## Key Takeaway

> `associateWith` is a powerful tool for converting Lists into Maps.  
> Chaining `filter` and `forEach` after it keeps the code clean and readable.