/*
 * Copyright 2003 - 2014 The eFaps Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("db37f9fd-a5ce-4feb-8c07-8932da68228a")
@EFapsRevision("$Rev$")
public abstract class InventoryReport_Base
    extends FilteredReport
{

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(InventoryReport.class.getName() + ".FileName"));
        File file = null;
        if ("xls".equalsIgnoreCase(mime)) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(mime)) {
            file = dyRp.getPDF(_parameter);
        }
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynInventoryReport(this);
    }

    /**
     * Report class.
     */
    public static class DynInventoryReport
        extends AbstractDynamicReport
    {

        /**
         * The related filtered report.
         */
        private final FilteredReport filteredReport;

        /**
         * @param _filteredReport reports
         */
        public DynInventoryReport(final InventoryReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Inventory inventory = new Inventory();
            inventory.setStorageInsts(getStorageInsts(_parameter));
            final List<InventoryBean> beans = inventory.getInventory(_parameter);
            Collections.sort(beans, getComparator(_parameter));

            return new JRBeanCollectionDataSource(beans);
        }

        protected ComparatorChain<InventoryBean> getComparator(final Parameter _parameter)
            throws EFapsException
        {
            final ComparatorChain<InventoryBean> ret = new ComparatorChain<>();
            if (isShowStorage(_parameter)) {
                ret.addComparator(new Comparator<InventoryBean>()
                {

                    @Override
                    public int compare(final InventoryBean _arg0,
                                       final InventoryBean _arg1)
                    {
                        return _arg0.getStorage().compareTo(_arg1.getStorage());
                    }
                });
            }

            ret.addComparator(new Comparator<InventoryBean>()
            {

                @Override
                public int compare(final InventoryBean _arg0,
                                   final InventoryBean _arg1)
                {
                    return _arg0.getProdName().compareTo(_arg1.getProdName());
                }
            });
            return ret;
        }

        protected boolean isShowStorage(final Parameter _parameter)
            throws EFapsException
        {
            return !getStorageInsts(_parameter).isEmpty();
        }

        protected List<Instance> getStorageInsts(final Parameter _parameter)
            throws EFapsException
        {
            final List<Instance> ret = new ArrayList<>();
            final Map<String, Object> map = getFilterMap(_parameter);
            if (map.containsKey("storageGroup")) {
                final InstanceFilterValue filter = (InstanceFilterValue) map.get("storageGroup");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    ret.addAll(new StorageGroup().getStorage4Group(_parameter, filter.getObject()));
                }
            }

            if (ret.isEmpty() && map.containsKey("storage")) {
                final InstanceFilterValue filter = (InstanceFilterValue) map.get("storage");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    ret.add(filter.getObject());
                }
            }
            return ret;
        }

        protected Map<String, Object> getFilterMap(final Parameter _parameter)
            throws EFapsException
        {
            return ((InventoryReport_Base) getFilteredReport()).getFilterMap(_parameter);
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> storageColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.storage"),
                            "storage", DynamicReports.type.stringType());

            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.prodName"),
                            "prodName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> prodDescrColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.prodDescr"),
                            "prodDescr", DynamicReports.type.stringType()).setWidth(200);
            final TextColumnBuilder<String> uoMColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.uoM"),
                            "uoM", DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.quantity"),
                            "quantity", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> reservedColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.reserved"),
                            "reserved", DynamicReports.type.bigDecimalType());

            if (isShowStorage(_parameter)) {
                _builder.addColumn(storageColumn);
                _builder.groupBy(storageColumn);
            }
            _builder.addColumn(prodNameColumn, prodDescrColumn, uoMColumn, quantityColumn, reservedColumn);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public FilteredReport getFilteredReport()
        {
            return this.filteredReport;
        }

    }

}
