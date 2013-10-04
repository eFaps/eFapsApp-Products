/*
 * Copyright 2003 - 2013 The eFaps Team
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
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */
package org.efaps.esjp.products.reports;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a6c66a7c-8f5c-45cc-a4d2-22160cbc6848")
@EFapsRevision("$Rev$")
public abstract class ExportKardexReport_Base
{

    public class KardexReport
        extends AbstractDynamicReport
    {

        private BigDecimal inventoryValue;

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DateTime dateFrom = new DateTime(_parameter
                            .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.dateFrom.name));

            final DateTime dateTo = new DateTime(_parameter
                            .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.dateTo.name));

            final Instance product = Instance.get(_parameter
                            .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.product.name));

            final Instance storage = Instance.get(_parameter
                            .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.storage.name));

            final boolean showPrice = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.showPrice.name));

            DRDataSource dataSource;
            if (showPrice) {
                dataSource = new DRDataSource("date", "inbound", "outbound", "inventory", "unitPrice", "uoM", "price");
            } else {
                dataSource = new DRDataSource("date", "inbound", "outbound", "inventory");
            }

            final List<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();
            if (product.isValid()) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, product.getId());
                queryBldr.addWhereAttrLessValue(CIProducts.TransactionInOutAbstract.Date, dateTo.plusSeconds(1));
                queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, dateFrom.minusSeconds(1));
                if (storage.isValid()) {
                    queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, storage.getId());
                }
                queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIProducts.TransactionInOutAbstract.Date,
                                CIProducts.TransactionInOutAbstract.Quantity,
                                CIProducts.TransactionInOutAbstract.UoM);
                multi.setEnforceSorted(true);
                multi.execute();
                addingRow2Map(lst, product, storage, dateFrom);
                while (multi.next()) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    final DateTime date = multi.<DateTime>getAttribute(CIProducts.TransactionInOutAbstract.Date);
                    BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.TransactionInOutAbstract.Quantity);
                    map.put("date", date.toDate());
                    if (CIProducts.TransactionInbound.getType().equals(multi.getCurrentInstance().getType())) {
                        map.put("inbound", quantity);
                    } else {
                        map.put("outbound", quantity.negate());
                        quantity = quantity.negate();
                    }
                    inventoryValue = inventoryValue.add(quantity);
                    map.put("inventory", inventoryValue);

                    if (showPrice) {
                        final Long uoMId = multi.<Long>getAttribute(CIProducts.TransactionInOutAbstract.UoM);
                        final UoM uoM = Dimension.getUoM(uoMId);
                        quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())
                                                        .divide(new BigDecimal(uoM.getDenominator())));
                        map.put("uoM", uoM.getName());

                        final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.ProductCost);
                        queryBldr2.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink, product.getId());
                        queryBldr2.addWhereAttrLessValue(CIProducts.ProductCost.ValidFrom, date.plusSeconds(1));
                        queryBldr2.addWhereAttrGreaterValue(CIProducts.ProductCost.ValidUntil, date.minusSeconds(1));
                        queryBldr2.addOrderByAttributeDesc(CIProducts.ProductCost.ID);
                        final MultiPrintQuery multi2 = queryBldr2.getPrint();
                        multi2.addAttribute(CIProducts.ProductCost.Price);
                        multi2.execute();
                        BigDecimal price = BigDecimal.ZERO;
                        if (multi2.next()) {
                            price = multi2.<BigDecimal>getAttribute(CIProducts.ProductCost.Price);
                        }
                        map.put("unitPrice", price);
                        map.put("price", quantity.multiply(price));
                    }
                    lst.add(map);
                }
                addingRow2Map(lst, product, storage, dateTo);
            }
            for (final Map<String, Object> map : lst) {
                if (showPrice) {
                    dataSource.add(map.get("date"), map.get("inbound"), map.get("outbound"), map.get("inventory"),
                                   map.get("unitPrice"), map.get("uoM"), map.get("price"));
                } else {
                    dataSource.add(map.get("date"), map.get("inbound"), map.get("outbound"), map.get("inventory"));
                }
            }
            return dataSource;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<Date> dateColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.products.reports.ExportKardexReport.Date"), "date",
                            DynamicReports.type.dateType());

            final TextColumnBuilder<BigDecimal> inboundColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.products.reports.ExportKardexReport.Inbound"), "inbound",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> outboundColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.products.reports.ExportKardexReport.Outbound"), "outbound",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> inventoryColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.products.reports.ExportKardexReport.Inventory"), "inventory",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> unitPriceColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.products.reports.ExportKardexReport.UnitPrice"), "unitPrice",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> uomColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.products.reports.ExportKardexReport.uoM"), "uoM",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> priceColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.products.reports.ExportKardexReport.Price"), "price",
                            DynamicReports.type.bigDecimalType());

            _builder.addColumn(dateColumn.setFixedWidth(80)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER),
                            inboundColumn.setFixedWidth(100),
                            outboundColumn.setFixedWidth(100),
                            inventoryColumn.setFixedWidth(100));

            final boolean showPrice = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.showPrice.name));

            if (showPrice) {
                _builder.addColumn(unitPriceColumn.setFixedWidth(50),
                                uomColumn.setFixedWidth(50).setHorizontalAlignment(HorizontalAlignment.RIGHT),
                                priceColumn.setFixedWidth(100));
            }
        }

        protected void addingRow2Map(final List<Map<String, Object>> _lst,
                                     final Instance _product,
                                     final Instance _storage,
                                     final DateTime _date)
            throws EFapsException
        {
            final Map<String, Object> addRows = new HashMap<String, Object>();
            final BigDecimal inventory = compareInventory4Transaction(getQuantity2Inventory(_product, _storage), _product, _date);
            addRows.put("inventory", inventory);
            _lst.add(addRows);

            inventoryValue = inventory;
        }
    }

    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        final Instance productInst = Instance.get(_parameter
                        .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.product.name));
        _parameter.put(ParameterValues.INSTANCE, productInst);

        final DateTime dateFrom = new DateTime(_parameter
                        .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.dateFrom.name));

        final DateTime dateTo = new DateTime(_parameter
                        .getParameterValue(CIFormProducts.Products_Kardex_ReportForm.dateTo.name));

        final String mime = _parameter.getParameterValue(CIFormProducts.Products_Kardex_ReportForm.mime.name);

        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.getReport().addParameter("FromDate", dateFrom.toDate());
        dyRp.getReport().addParameter("ToDate", dateTo.toDate());
        dyRp.setFileName(DBProperties.getProperty("org.efaps.esjp.products.reports.Kardex"));

        File file = null;
        if ("xls".equalsIgnoreCase(mime)) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(mime)){
            file = dyRp.getPDF(_parameter);
        }

        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new KardexReport();
    }

    protected BigDecimal getQuantity2Inventory(final Instance _product,
                                               final Instance _storage)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        if (_product != null && _product.isValid()) {
            queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, _product.getId());
        }
        if (_storage != null && _storage.isValid()) {
            queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, _storage.getId());
        }
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity,
                        CIProducts.Inventory.UoM);
        multi.execute();
        while (multi.next()) {
            BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            final Long uoMId = multi.<Long>getAttribute(CIProducts.Inventory.UoM);
            final UoM uoM = Dimension.getUoM(uoMId);
            quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())
                            .divide(new BigDecimal(uoM.getDenominator())));
            ret = ret.add(quantity);
        }
        return ret;
    }

    protected BigDecimal compareInventory4Transaction(final BigDecimal _inventory,
                                                      final Instance _product,
                                                      final DateTime _date)
        throws EFapsException
    {
        BigDecimal ret = _inventory;
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, _product.getId());
        queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, _date.minusMinutes(1));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.TransactionInOutAbstract.Quantity,
                        CIProducts.TransactionInOutAbstract.UoM);
        multi.execute();
        while (multi.next()) {
            BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.TransactionInbound.Quantity);
            final Long uoMId = multi.<Long>getAttribute(CIProducts.TransactionInbound.UoM);
            final UoM uoM = Dimension.getUoM(uoMId);
            quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())
                                            .divide(new BigDecimal(uoM.getDenominator())));
            final Instance inst = multi.getCurrentInstance();
            if (inst.getType().isKindOf(CIProducts.TransactionInbound.getType())) {
                quantity = quantity.negate();
            }
            ret = ret.add(quantity);
        }
        return ret;
    }
}
