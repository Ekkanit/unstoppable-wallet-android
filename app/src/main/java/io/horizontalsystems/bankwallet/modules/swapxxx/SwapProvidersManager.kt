package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataField
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class SwapProvidersManager {
    private val allProviders = listOf(
        SwapMainModule.OneInchProvider,
        SwapMainModule.PancakeSwapProvider,
        SwapMainModule.PancakeSwapV3Provider,
        SwapMainModule.QuickSwapProvider,
        SwapMainModule.UniswapProvider,
        SwapMainModule.UniswapV3Provider,
    )

    suspend fun getQuotes(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal) = coroutineScope {
        val providers = allProviders.filter {
            it.supports(tokenIn, tokenOut)
        }
        providers
            .map { provider ->
                async {
                    try {
                        val quote = provider.fetchQuote(tokenIn, tokenOut, amountIn)
                        SwapProviderQuote(
                            provider,
                            quote.amountOut,
                            quote.fee,
                            quote.fields
                        )
                    } catch (e: Throwable) {
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
    }
}

data class SwapProviderQuote(
    val provider: SwapMainModule.ISwapProvider,
    val amountOut: BigDecimal,
    val fee: SendModule.AmountData?,
    val fields: List<SwapDataField>
)
