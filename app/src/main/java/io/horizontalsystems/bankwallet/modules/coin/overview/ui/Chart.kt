package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.chart.SelectedPoint
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartDataItemImmutable
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod

@Composable
fun HsChartLineHeader(
    chartHeaderView: ChartModule.ChartHeaderView?,
    selectedPoint: SelectedPoint?
) {
    RowUniversal(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        if (selectedPoint == null) {
            Text(
                modifier = Modifier.padding(end = 8.dp),
                text = chartHeaderView?.value ?: "--",
                style = ComposeAppTheme.typography.headline1,
                color = ComposeAppTheme.colors.leah
            )
            (chartHeaderView as? ChartModule.ChartHeaderView.Latest)?.let { latest ->
                Text(
                    text = formatValueAsDiff(latest.diff),
                    style = ComposeAppTheme.typography.subhead1,
                    color = diffColor(latest.diff.raw())
                )
            }
        } else {
            Column {
                captionSB_leah(text = selectedPoint.value)
                Spacer(modifier = Modifier.height(4.dp))
                caption_grey(text = selectedPoint.date)
            }

            Spacer(modifier = Modifier.weight(1f))

            when (val extraData = selectedPoint.extraData) {
                is SelectedPoint.ExtraData.Volume -> {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        caption_grey(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.CoinPage_Volume),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        caption_grey(
                            modifier = Modifier.fillMaxWidth(),
                            text = extraData.volume,
                            textAlign = TextAlign.End
                        )
                    }
                }
                is SelectedPoint.ExtraData.Dominance -> {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        caption_grey(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.Market_BtcDominance),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        caption_jacob(
                            modifier = Modifier.fillMaxWidth(),
                            text = extraData.dominance,
                            textAlign = TextAlign.End
                        )
                    }
                }
                null ->{}
            }
        }
    }
}

@Composable
fun Chart(chartViewModel: ChartViewModel, onSelectChartInterval: ((HsTimePeriod?) -> Unit)? = null) {
    val chartDataWrapper by chartViewModel.dataWrapperLiveData.observeAsState()
    val chartTabs by chartViewModel.tabItemsLiveData.observeAsState(listOf())
    val chartLoading by chartViewModel.loadingLiveData.observeAsState(false)
    val chartViewState by chartViewModel.viewStateLiveData.observeAsState()

    Column {
        var selectedPoint by remember { mutableStateOf<SelectedPoint?>(null) }

        HsChartLineHeader(chartDataWrapper?.chartHeaderView, selectedPoint)
        Chart(
            tabItems = chartTabs,
            onSelectTab = {
                chartViewModel.onSelectChartInterval(it)
                onSelectChartInterval?.invoke(it)
            },
            chartInfoData = chartDataWrapper?.chartInfoData,
            chartLoading = chartLoading,
            viewState = chartViewState,
            hasVolumes = chartViewModel.hasVolumes
        ) { item ->
            selectedPoint = item?.let {
                chartViewModel.getSelectedPoint(it)
            }
        }
    }
}

@Composable
fun <T> Chart(
    tabItems: List<TabItem<T>>,
    onSelectTab: (T) -> Unit,
    chartInfoData: ChartInfoData?,
    chartLoading: Boolean,
    viewState: ViewState?,
    hasVolumes: Boolean,
    onSelectPoint: (ChartDataItemImmutable?) -> Unit,
) {
    Column {
        PriceVolChart(
            chartInfoData = chartInfoData,
            loading = chartLoading,
            viewState = viewState,
            onSelectPoint = onSelectPoint,
            hasVolumes = hasVolumes
        )
        VSpacer(height = 8.dp)
        ChartTab(
            tabItems = tabItems,
            onSelect = onSelectTab
        )
    }
}

@Composable
fun PriceVolChart(
    chartInfoData: ChartInfoData?,
    loading: Boolean,
    viewState: ViewState?,
    onSelectPoint: (ChartDataItemImmutable?) -> Unit,
    hasVolumes: Boolean,
) {
    val height = if (hasVolumes) 204.dp else 160.dp

    AndroidView(
        modifier = Modifier
            .height(height)
            .fillMaxWidth(),
        factory = {
            Chart(it).apply {
                setListener(object : Chart.Listener {
                    override fun onTouchDown() {
                    }

                    override fun onTouchUp() {
                        onSelectPoint.invoke(null)
                    }

                    override fun onTouchSelect(item: ChartDataItemImmutable) {
                        onSelectPoint.invoke(item)
                        HudHelper.vibrate(context)
                    }
                })
            }
        },
        update = { chart ->
            if (loading) {
                chart.showSpinner()
            } else {
                chart.hideSpinner()
            }

            when (viewState) {
                is ViewState.Error -> {
                    chart.showError(viewState.t.localizedMessage ?: "")
                }
                ViewState.Success -> {
                    chart.hideError()
                    chart.setIndicatorLineVisible(hasVolumes)

                    chartInfoData?.let { chartInfoData ->
                        chart.doOnLayout {
                            if (chartInfoData.chartData.isMovementChart) {
                                chart.setData(chartInfoData.chartData, chartInfoData.maxValue, chartInfoData.minValue)
                            } else {
                                chart.setDataBars(chartInfoData.chartData, chartInfoData.maxValue, chartInfoData.minValue)
                            }
                        }
                    }
                }
                ViewState.Loading,
                null -> {}
            }
        }
    )
}

@Composable
fun <T> ChartTab(modifier: Modifier = Modifier, tabItems: List<TabItem<T>>, onSelect: (T) -> Unit) {
    val tabIndex = tabItems.indexOfFirst { it.selected }

    TabPeriod(modifier = modifier) {
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            modifier = Modifier,
            backgroundColor = Color.Transparent,
            edgePadding = 0.dp,
            indicator = {},
            divider = {}
        ) {
            tabItems.forEachIndexed { index, tabItem ->
                val selected = tabIndex == index

                Tab(
                    selected = selected,
                    onClick = { },
                ) {
                    TabButtonSecondaryTransparent(
                        title = tabItem.title,
                        onSelect = {
                            onSelect.invoke(tabItem.item)
                        },
                        selected = selected
                    )
                }
            }
        }
    }
}
