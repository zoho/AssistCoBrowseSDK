package com.zoho.assist.cobrowse.demo.bank

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every model the bank app renders, plus the in-memory data behind them.
 *
 * The app makes no network calls: a fresh clone builds and runs with this file
 * as its only source of truth. Everything is generated deterministically (no
 * `Random`), so a screenshot taken on one run matches the next.
 */

enum class Category(val label: String, val color: Color, val icon: ImageVector) {
    SHOPPING("Shopping", Color(0xFF4A88A1), Icons.Filled.ShoppingCart),
    BABY("Childcare", Color(0xFF36978F), Icons.Filled.ChildFriendly),
    HOME("Bills & home", Color(0xFF94A05A), Icons.Filled.Home),
    FOOD("Coffee", Color(0xFFC58E5A), Icons.Filled.LocalCafe),
    TRANSPORT("Transport", Color(0xFF6E7AC4), Icons.Filled.DirectionsCar),
    DINING("Dining", Color(0xFFB85A7A), Icons.Filled.Restaurant),
}

data class Transaction(
    val id: Int,
    val payee: String,
    val dateLabel: String,
    val amount: Double,
    val category: Category,
)

data class Account(
    val name: String,
    val accountNumberMasked: String,
    val balance: Double,
    val isCredit: Boolean = false,
    val sortCode: String = "20-45-19",
    val iban: String = "GB29 ASBK 2045 1900 0000 00",
)

data class PaymentCard(
    val nickname: String,
    val number: String,
    val expiry: String,
    val cvv: String,
    val network: String,
    val gradient: List<Color>,
)

/** A saved beneficiary, offered as a shortcut on the transfer form. */
data class Payee(
    val name: String,
    val accountNumber: String,
    val bank: String,
)

/** One row of the live-markets ticker on the More tab. */
data class Ticker(val symbol: String, val name: String, val price: Double, val changePct: Double)

object BankData {

    const val CUSTOMER_NAME = "Alex Morgan"
    const val BANK_NAME = "Assist Bank"

    val transactions: List<Transaction> = listOf(
        Transaction(1, "Tutor Time", "14th at 14:49", 187.40, Category.BABY),
        Transaction(2, "Comcast", "5th at 13:30", 110.50, Category.HOME),
        Transaction(3, "Tesco", "5th at 01:31", 186.22, Category.SHOPPING),
        Transaction(4, "Starbucks", "3rd at 09:12", 6.85, Category.FOOD),
        Transaction(5, "Uber", "2nd at 22:04", 18.30, Category.TRANSPORT),
        Transaction(6, "Whole Foods", "1st at 17:48", 72.18, Category.SHOPPING),
        Transaction(7, "Olive Garden", "April 28 at 19:55", 54.27, Category.DINING),
        Transaction(8, "Daycare Co-op", "April 26 at 08:00", 220.00, Category.BABY),
        Transaction(9, "Shell", "April 25 at 18:22", 48.10, Category.TRANSPORT),
        Transaction(10, "Trader Joe's", "April 24 at 11:05", 64.92, Category.SHOPPING),
        Transaction(11, "Netflix", "April 23 at 02:10", 15.99, Category.HOME),
        Transaction(12, "Chipotle", "April 22 at 12:48", 14.20, Category.DINING),
        Transaction(13, "Lyft", "April 21 at 23:11", 22.75, Category.TRANSPORT),
        Transaction(14, "Spotify", "April 20 at 06:00", 10.99, Category.HOME),
        Transaction(15, "CVS Pharmacy", "April 19 at 16:34", 32.40, Category.SHOPPING),
        Transaction(16, "Peet's Coffee", "April 18 at 07:55", 5.75, Category.FOOD),
        Transaction(17, "Petco", "April 17 at 14:02", 41.18, Category.SHOPPING),
        Transaction(18, "Sushi Garden", "April 16 at 20:18", 68.40, Category.DINING),
        Transaction(19, "Babysitter", "April 15 at 21:30", 95.00, Category.BABY),
        Transaction(20, "PG&E", "April 14 at 09:00", 142.30, Category.HOME),
        Transaction(21, "Target", "April 13 at 13:44", 88.27, Category.SHOPPING),
        Transaction(22, "BART", "April 12 at 08:15", 6.40, Category.TRANSPORT),
        Transaction(23, "Blue Bottle", "April 11 at 10:30", 7.20, Category.FOOD),
        Transaction(24, "Amazon", "April 10 at 19:02", 124.55, Category.SHOPPING),
        Transaction(25, "Five Guys", "April 9 at 19:48", 24.90, Category.DINING),
    )

    val balance: Double = 2786.57
    val spentThisMonth: Double = 537.53

    val categoryShares: List<Pair<Category, Float>> = listOf(
        Category.SHOPPING to 0.40f,
        Category.BABY to 0.30f,
        Category.HOME to 0.20f,
        Category.FOOD to 0.10f,
    )

    /** The accounts shown on Home; [allAccounts] is the full list behind the Accounts tab. */
    val primaryAccounts: List<Account> = listOf(
        Account("Everyday Checking", "•••• 4421", 2_786.57),
        Account("Vault Savings", "•••• 8830", 18_204.10, sortCode = "20-45-22"),
        Account("Travel Rewards Card", "•••• 1170", -432.18, isCredit = true, sortCode = "20-45-30"),
    )

    /**
     * 100 accounts for the Accounts tab — a long, scrollable list so row
     * recycling is exercised under fast scroll.
     */
    private val accountTypes = listOf(
        "Everyday Checking", "Vault Savings", "Travel Rewards", "Cashback Card",
        "Joint Checking", "Emergency Fund", "Brokerage", "HSA", "Kids Savings",
        "Mortgage", "Auto Loan", "Business Checking",
    )
    val allAccounts: List<Account> = (1..100).map { i ->
        val isCredit = i % 6 == 0
        Account(
            name = "${accountTypes[i % accountTypes.size]} #$i",
            accountNumberMasked = "•••• ${(1000 + i * 7) % 9000 + 1000}",
            balance = if (isCredit) -((i * 37) % 900 + 50.13) else ((i * 911) % 24000 + 120.45),
            isCredit = isCredit,
            sortCode = "20-45-${(10 + i % 80).toString().padStart(2, '0')}",
        )
    }

    val cards: List<PaymentCard> = listOf(
        PaymentCard(
            nickname = "Travel", number = "4929 8821 6440 1170", expiry = "08/27",
            cvv = "419", network = "VISA",
            gradient = listOf(Color(0xFF1A237E), Color(0xFF42A5F5)),
        ),
        PaymentCard(
            nickname = "Everyday", number = "5371 0299 1248 4421", expiry = "11/26",
            cvv = "208", network = "MASTERCARD",
            gradient = listOf(Color(0xFFB71C1C), Color(0xFFFF8A65)),
        ),
        PaymentCard(
            nickname = "Business", number = "3782 8224 6310 0058", expiry = "02/28",
            cvv = "7712", network = "AMEX",
            gradient = listOf(Color(0xFF00695C), Color(0xFF4DB6AC)),
        ),
    )

    val payees: List<Payee> = listOf(
        Payee("Jordan Reyes", "8842 6610 4471", "Assist Bank"),
        Payee("Priya Nair", "5520 1194 8830", "Northgate Savings"),
        Payee("Landlord — 14B", "6611 2093 7781", "Civic Union Bank"),
        Payee("Daycare Co-op", "9930 4417 2265", "Assist Bank"),
    )

    val tickers: List<Ticker> = listOf(
        Ticker("ASBK", "Assist Bank Group", 148.22, 1.34),
        Ticker("NGSV", "Northgate Savings", 62.08, -0.42),
        Ticker("USDX", "Dollar index", 104.71, 0.18),
        Ticker("GOLD", "Gold spot / oz", 2318.40, -0.86),
    )

    /**
     * 1000 posted entries for the full statement history. Generated from the
     * indices so the order is stable across runs and every row has a distinct
     * key for `LazyColumn`.
     */
    private val historyPayees = listOf(
        "Tesco", "Amazon", "Shell", "Starbucks", "Netflix", "Uber", "Target",
        "Whole Foods", "PG&E", "Chipotle", "Lyft", "Spotify", "CVS Pharmacy",
        "Blue Bottle", "Petco", "Sushi Garden", "Comcast", "Five Guys",
    )
    val history: List<Transaction> = (1..1000).map { i ->
        Transaction(
            id = i,
            payee = historyPayees[i % historyPayees.size],
            dateLabel = "Day ${(i % 28) + 1} at ${(i % 24).toString().padStart(2, '0')}:${(i * 7 % 60).toString().padStart(2, '0')}",
            amount = (i * 3.5) % 900 + 5,
            category = Category.entries[i % Category.entries.size],
        )
    }
}

/** `1234.5` → `"$1,234.50"`, negatives as `"-$..."`. Locale-free so output is stable. */
internal fun formatAmount(value: Double): String {
    val rounded = (value * 100).toLong()
    val negative = rounded < 0
    val abs = if (negative) -rounded else rounded
    val whole = abs / 100
    val cents = (abs % 100).toString().padStart(2, '0')
    val withCommas = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return (if (negative) "-$" else "$") + withCommas + "." + cents
}
