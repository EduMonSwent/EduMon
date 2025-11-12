package com.android.sample.data.shop

// Parts of this file were generated with the help of an AI language model.

/** Provider to resolve the concrete repository from the app code */
object ShopRepositoryProvider {
    val repository: ShopRepository by lazy { FirestoreShopDataSource() }
}
