/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.efaps.esjp.products.dashboard;

import com.mchange.v1.lang.BooleanUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.esjp.common.dashboard.AbstractDashboardPanel;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.TreeView;
import org.efaps.esjp.ui.html.dojo.charting.Axis;
import org.efaps.esjp.ui.html.dojo.charting.ColumnsChart;
import org.efaps.esjp.ui.html.dojo.charting.Data;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.PlotLayout;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.esjp.ui.html.dojo.charting.Util;
import org.efaps.util.EFapsBaseException;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;

/**
 * Dashboard panel that renders a stacked column for inventory.
 *
 * @author The eFaps Team
 */
@EFapsUUID("7bbd1fb7-d9c8-4db3-a18b-54af589237d1")
@EFapsApplication("eFapsApp-Products")
public abstract class InventoryValuePanel_Base
    extends AbstractDashboardPanel
    implements IEsjpSnipplet
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new inventory value panel_ base.
     */
    public InventoryValuePanel_Base()
    {
        super();
    }

    /**
     * Instantiates a new inventory value panel_ base.
     *
     * @param _config the _config
     */
    public InventoryValuePanel_Base(final String _config)
    {
        super(_config);
    }

    /**
     * Gets the ClassificationLevel. 0 deactivates. 1 means classification on
     * first level,
     *
     * @return the width
     */
    protected Integer getClassificationLevel()
    {
        return Integer.valueOf(getConfig().getProperty("ClassificationLevel", "0"));
    }

    /**
     * Gets the currency inst.
     *
     * @return the currency inst
     * @throws EFapsException the e faps exception
     */
    protected Instance getCurrencyInst()
        throws EFapsException
    {
        return Instance.get(getConfig().getProperty("CurrencyOID", Currency.getBaseCurrency().getOid()));
    }

    /**
     * Checks if is active.
     *
     * @return true, if is active
     */
    protected boolean isAlternative()
    {
        return BooleanUtils.parseBoolean(getConfig().getProperty("Alternative", "false"));
    }

    /**
     * Gets the currency inst.
     *
     * @return the currency inst
     * @throws EFapsException the e faps exception
     */
    protected Instance getTreeViewInst()
        throws EFapsException
    {
        return Instance.get(getConfig().getProperty("TreeViewOID"));
    }

    /**
     * Gets the currency inst.
     *
     * @return the currency inst
     * @throws EFapsException the e faps exception
     */
    protected String getDateFormat()
        throws EFapsException
    {
        return getConfig().getProperty("DateFormat", "dd/MM/yyyy");
    }

    /**
     * Gets the duration.
     *
     * @return the duration
     */
    protected String getDuration()
    {
        return getConfig().getProperty("Duration", "MONTH");
    }

    /**
     * Gets the duration quantity.
     *
     * @return the duration quantity
     */
    protected Integer getDurationQuantity()
    {
        return Integer.valueOf(getConfig().getProperty("DurationQuantity", "3"));
    }

    /**
     * Gets the duration quantity.
     *
     * @return the duration quantity
     */
    protected String getDurationDate()
    {
        return getConfig().getProperty("DurationDate", "LAST");
    }

    /**
     * Gets the y axis min.
     *
     * @return the y axis min
     */
    protected Integer getYAxisMin()
    {
        return Integer.valueOf(getConfig().getProperty("YAxisMin", "0"));
    }

    /**
     * Gets the y axis max.
     *
     * @return the y axis max
     */
    protected Integer getYAxisMax()
    {
        return getConfig().containsKey("YAxisMax") ? Integer.valueOf(getConfig().getProperty("YAxisMax")) : null;
    }

    @Override
    public CharSequence getHtmlSnipplet()
        throws EFapsBaseException
    {
        CharSequence ret;
        if (isCached()) {
            ret = getFromCache();
        } else {
            final ColumnsChart chart = new ColumnsChart().setPlotLayout(PlotLayout.STACKED)
                            .setWidth(getWidth()).setHeight(getHeight()).setGap(5);
            final String title = getTitle();
            if (title != null && !title.isEmpty()) {
                chart.setTitle(getTitle());
            }
            chart.setOrientation(Orientation.VERTICAL_CHART_LEGEND);
            final Axis xAxis = new Axis().setName("x");
            chart.addAxis(xAxis);
            chart.addAxis(new Axis().setName("y").setVertical(true).setMin(getYAxisMin()).setMax(getYAxisMax()));

            final List<Map<String, Object>> labels = new ArrayList<>();
            final List<DateTime> dates = new ArrayList<>();
            final String durationDate = getDurationDate();
            for (int i = getDurationQuantity(); i > 0; i--) {
                DateTime date;
                switch (getDuration()) {
                    case "YEAR":
                        date = new DateTime().withTimeAtStartOfDay().withFieldAdded(DurationFieldType.years(), -i);
                        break;
                    case "HALFYEAR":
                        date = new DateTime().withTimeAtStartOfDay().withFieldAdded(JodaTimeUtils.halfYears(), -i);
                        break;
                    case "QUARTER":
                        date = new DateTime().withTimeAtStartOfDay().withFieldAdded(JodaTimeUtils.quarters(), -i);
                        break;
                    case "WEEK":
                        date = new DateTime().withTimeAtStartOfDay().withFieldAdded(DurationFieldType.weeks(), -i);
                        break;
                    case "DAY":
                        date = new DateTime().withTimeAtStartOfDay().withFieldAdded(DurationFieldType.days(), -i);
                        break;
                    case "MONTH":
                    default:
                        date = new DateTime().withTimeAtStartOfDay().withFieldAdded(DurationFieldType.months(), -i);
                        if ("LAST".equalsIgnoreCase(durationDate)) {
                            date = date.plusMonths(1).withDayOfMonth(1).minusDays(1);
                        }
                        break;
                }
                dates.add(date);
            }
            dates.add(new DateTime().withTimeAtStartOfDay());

            final Map<String, Serie<Data>> series = new HashMap<>();
            int xValue = 0;
            for (final DateTime date : dates) {
                xValue++;

                final Parameter parameter = new Parameter();
                final Inventory inventory = new Inventory()
                        .setDate(date)
                        .setShowProdClass(getClassificationLevel() > 0)
                        .setCurrencyInst(getCurrencyInst())
                        .setAlternativeCurrencyInst(isAlternative() ? getCurrencyInst() : null);

                final Map<String, Object> map = new HashMap<>();
                map.put("value", xValue);
                map.put("text", Util.wrap4String(date.toString(getDateFormat(),
                                Context.getThreadContext().getLocale())));
                labels.add(map);

                final List<? extends InventoryBean> beans = inventory.getInventory(parameter);

                final Map<String, BigDecimal> values = new HashMap<>();
                BigDecimal total = BigDecimal.ZERO;
                for (final InventoryBean bean : beans) {
                    String serieName;
                    if (getTreeViewInst().isValid()) {
                        serieName = TreeView.getTreeViewLabel(new Parameter(),
                                        getTreeViewInst(), bean.getProdInstance(), true, true);
                        serieName = serieName == null
                                    ? DBProperties.getProperty(InventoryValuePanel.class.getName() + ".WithoutTreeView")
                                    : serieName;
                    } else if (getClassificationLevel() > 0) {
                        serieName = bean.getProdClass(getClassificationLevel());
                    } else {
                        serieName = bean.getProdType();
                    }
                    if (!values.containsKey(serieName)) {
                        values.put(serieName, BigDecimal.ZERO);
                    }
                    values.put(serieName, values.get(serieName).add(bean.getTotal()));
                    total = total.add(bean.getTotal());
                }

                final DecimalFormat fmtr = NumberFormatter.get().getFormatter();
                for (final Entry<String, BigDecimal> value : values.entrySet()) {
                    if (value.getValue().compareTo(BigDecimal.ZERO) > 0) {
                        final Serie<Data> serie;
                        if (series.containsKey(value.getKey())) {
                            serie = series.get(value.getKey());
                        } else {
                            serie = new Serie<>();
                            series.put(value.getKey(), serie);
                            serie.setName(value.getKey());
                            chart.addSerie(serie);
                        }
                        final Data data = new Data().setXValue(xValue).setYValue(value.getValue()).setSimple(false);
                        data.setTooltip(serie.getName() + " " + fmtr.format(value.getValue())
                                + " / " + fmtr.format(total));
                        serie.addData(data);
                    }
                }
            }
            // ensure that all series have values for all
            for (final Serie<Data> serie : series.values()) {
                if (serie.getData().size() < xValue) {
                    for (int i = 1; i < xValue + 1; i++) {
                        final int idx = i;
                        if (!serie.getData().stream()
                                        .filter(data -> (data.getXValue().intValue() == idx))
                                        .findFirst()
                                        .isPresent()) {
                            final Data data = new Data().setXValue(idx).setYValue(0).setSimple(false);
                            data.setTooltip(serie.getName());
                            serie.addData(data);
                        }
                    }
                }
            }
            xAxis.setLabels(Util.mapCollectionToObjectArray(labels));
            ret = chart.getHtmlSnipplet();
            cache(ret);
        }
        return ret;
    }

    @Override
    public boolean isVisible()
        throws EFapsBaseException
    {
        return true;
    }
}
