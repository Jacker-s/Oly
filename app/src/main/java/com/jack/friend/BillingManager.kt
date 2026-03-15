package com.jack.friend

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager(private val context: Context) {

    private val TAG = "BillingManager"

    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            Log.d(TAG, "onPurchasesUpdated: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
        .enablePendingPurchases()
        .build()

    private val _isPremiumPurchased = MutableStateFlow(false)
    val isPremiumPurchased: StateFlow<Boolean> = _isPremiumPurchased

    init {
        Log.d(TAG, "Inicializando BillingManager...")
        startConnection()
    }

    private fun startConnection() {
        Log.d(TAG, "Iniciando conexão com Google Play...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Client conectado com sucesso")
                    queryPurchases()
                } else {
                    Log.e(TAG, "Erro ao conectar Billing Client: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing Client desconectado. Tentando reconectar...")
                startConnection()
            }
        })
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryPurchases: BillingClient não está pronto")
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any { purchase ->
                    purchase.products.contains("com.wappi.messenger.remove_ads") && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPremiumPurchased.value = hasPremium
                Log.d(TAG, "Status Premium consultado: $hasPremium")
            } else {
                Log.e(TAG, "Erro ao consultar compras: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isPremiumPurchased.value = true
                        Log.d(TAG, "Compra confirmada e reconhecida")
                    }
                }
            } else {
                _isPremiumPurchased.value = true
                Log.d(TAG, "Compra já estava reconhecida")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String = "com.wappi.messenger.remove_ads") {
        Log.d(TAG, "launchPurchaseFlow chamado para o produto: $productId")
        
        if (!billingClient.isReady) {
            Log.e(TAG, "launchPurchaseFlow: BillingClient não está pronto")
            startConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        Log.d(TAG, "Buscando detalhes do produto na loja...")
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            Log.d(TAG, "Resultado da busca de detalhes: ${billingResult.responseCode}")
            
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList[0]
                    Log.d(TAG, "Produto encontrado: ${productDetails.name} - ${productDetails.oneTimePurchaseOfferDetails?.formattedPrice}")
                    
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                    activity.runOnUiThread {
                        Log.d(TAG, "Iniciando tela de pagamento do Google Play...")
                        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
                        Log.d(TAG, "Resultado do launchBillingFlow: ${result.responseCode}")
                    }
                } else {
                    Log.e(TAG, "LISTA DE PRODUTOS VAZIA. O ID '$productId' não foi retornado pela Google Play.")
                    activity.runOnUiThread {
                        android.widget.Toast.makeText(activity, "Produto não encontrado. Verifique se o ID está correto no Console.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Log.e(TAG, "Erro ao buscar detalhes do produto: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }
}
