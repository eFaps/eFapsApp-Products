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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

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
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: InventoryReport_Base.java 14621 2014-12-15 00:12:35Z
 *          jan@moxter.net $
 */
@EFapsUUID("db37f9fd-a5ce-4feb-8c07-8932da68228a")
@EFapsRevision("$Rev$")
public abstract class InventoryReport_Base
    extends FilteredReport
{

    public enum StorageDisplay
    {
        NONE, COLUMN, ROW;
    }

    public enum TypeDisplay
    {
        NONE, COLUMN, GROUP;
    }

    public enum ClassDisplay
    {
        NONE, COLUMN, GROUP;
    }

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

        private List<InventoryBean> beans;

        /**
         * @param _filteredReport reports
         */
        public DynInventoryReport(final FilteredReport _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRDataSource ret;
            if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                final List<Map<String, ?>> source = getMapList(_parameter);
                ret = new JRMapCollectionDataSource(source);
            } else {
                Collections.sort(getBeans(_parameter, null), getComparator(_parameter));
                ret = new JRBeanCollectionDataSource(this.beans);
            }
            return ret;
        }

        protected List<Map<String, ?>> getMapList(final Parameter _parameter)
            throws EFapsException
        {
            final List<Map<String, ?>> ret = new ArrayList<>();
            final List<InventoryBean> tmpBeans = getBeans(_parameter, null);
            Collections.sort(tmpBeans, getComparator(_parameter));
            Map<String, Object> map = new HashMap<>();
            BigDecimal quantity = BigDecimal.ZERO;
            for (final InventoryBean bean : tmpBeans) {
                if (!bean.getProdName().equals(map.get("prodName"))) {
                    map = new HashMap<>();
                    quantity = BigDecimal.ZERO;
                    ret.add(map);
                    map.put("prodType", bean.getProdType());
                    map.put("prodClass", bean.getProdClass());
                    map.put("prodName", bean.getProdName());
                    map.put("prodDescr", bean.getProdDescr());
                    map.put("prodOID", bean.getProdOID());
                    map.put("currency", bean.getCurrency());
                    map.put("prodName", bean.getProdName());
                    map.put("cost", bean.getCost());
                    map.put("uoM", bean.getUoM());
                }
                quantity = quantity.add(bean.getQuantity()).add(bean.getReserved());
                map.put("total", bean.getCost().multiply(quantity));
                map.put(bean.getStorage() + "_quantity", bean.getQuantity());
                map.put(bean.getStorage() + "_reserved", bean.getReserved());
            }
            return ret;
        }

        protected Inventory getInventoryObject(final Parameter _parameter)
        {
            return new Inventory();
        }

        protected ComparatorChain<InventoryBean> getComparator(final Parameter _parameter)
            throws EFapsException
        {
            final ComparatorChain<InventoryBean> ret = new ComparatorChain<>();
            if (StorageDisplay.ROW.equals(getStorageDisplay(_parameter))) {
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

        protected StorageDisplay getStorageDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilterMap(_parameter).get("storageDisplay");
            StorageDisplay ret;
            if (filter != null) {
                ret = (StorageDisplay) filter.getObject();
            } else {
                ret = StorageDisplay.NONE;
            }
            return ret;
        }


        protected TypeDisplay getTypeDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilterMap(_parameter).get("typeDisplay");
            TypeDisplay ret;
            if (filter != null) {
                ret = (TypeDisplay) filter.getObject();
            } else {
                ret = TypeDisplay.NONE;
            }
            return ret;
        }

        protected ClassDisplay getClassDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilterMap(_parameter).get("classDisplay");
            ClassDisplay ret;
            if (filter != null) {
                ret = (ClassDisplay) filter.getObject();
            } else {
                ret = ClassDisplay.NONE;
            }
            return ret;
        }

        protected boolean isEvaluateCost(final Parameter _parameter)
            throws EFapsException
        {
            return "true".equalsIgnoreCase(getProperty(_parameter, "EvaluateCost"));
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
            final List<ColumnTitleGroupBuilder> groupBuilders = new ArrayList<>();
            ColumnGroupBuilder storageSumGroup = null;
            if (StorageDisplay.ROW.equals(getStorageDisplay(_parameter))) {
                final TextColumnBuilder<String> storageColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.storage"),
                                "storage", DynamicReports.type.stringType());
                _builder.addColumn(storageColumn);
                storageSumGroup = DynamicReports.grp.group(storageColumn);
                _builder.groupBy(storageSumGroup);
            }

            final ColumnTitleGroupBuilder prodGroup = DynamicReports.grid.titleGroup(
                            DBProperties.getProperty(InventoryReport.class.getName() + ".TitleGroup.product"));

            ColumnGroupBuilder typeGroup = null;
            if (!TypeDisplay.NONE.equals(getTypeDisplay(_parameter))) {
                final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.prodType"),
                                "prodType", DynamicReports.type.stringType()).setWidth(150);
                _builder.addColumn(typeColumn);

                if (TypeDisplay.GROUP.equals(getTypeDisplay(_parameter))) {
                    typeGroup = DynamicReports.grp.group(typeColumn);
                    _builder.groupBy(typeGroup);
                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    prodGroup.add(typeColumn);
                }
            }
            ColumnGroupBuilder classGroup = null;
            if (!ClassDisplay.NONE.equals(getClassDisplay(_parameter))) {
                final TextColumnBuilder<String> classColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.prodClass"),
                                "prodClass", DynamicReports.type.stringType()).setWidth(250);
                _builder.addColumn(classColumn);

                if (ClassDisplay.GROUP.equals(getClassDisplay(_parameter))) {
                    classGroup = DynamicReports.grp.group(classColumn);
                    _builder.groupBy(classGroup);
                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    prodGroup.add(classColumn);
                }
            }

            if (getExType().equals(ExportType.HTML)) {
                final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                                "http://www.efaps.org", "efapslink")
                                .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new ProductLinkExpression())
                                .setHeight(12).setWidth(25);
                final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");
                _builder.addColumn(linkColumn);
                prodGroup.add(linkColumn);
            }

            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.prodName"),
                            "prodName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> prodDescrColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.prodDescr"),
                            "prodDescr", DynamicReports.type.stringType()).setWidth(200);
            final TextColumnBuilder<String> uoMColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.uoM"),
                            "uoM", DynamicReports.type.stringType());
            _builder.addColumn(prodNameColumn, prodDescrColumn, uoMColumn);

            if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                prodGroup.add(prodNameColumn, prodDescrColumn, uoMColumn);
                groupBuilders.add(prodGroup);
                for (final String storage : getStorages(_parameter)) {
                    final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                                    .getProperty(InventoryReport.class.getName() + ".Column.quantity"),
                                    storage + "_quantity", DynamicReports.type.bigDecimalType());
                    final TextColumnBuilder<BigDecimal> reservedColumn = DynamicReports.col.column(DBProperties
                                    .getProperty(InventoryReport.class.getName() + ".Column.reserved"),
                                    storage + "_reserved", DynamicReports.type.bigDecimalType());
                    final ColumnTitleGroupBuilder storageGroup = DynamicReports.grid.titleGroup(storage, quantityColumn,
                                    reservedColumn);
                    groupBuilders.add(storageGroup);
                    _builder.addColumn(quantityColumn, reservedColumn);
                }

            } else {
                final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.quantity"),
                            "quantity", DynamicReports.type.bigDecimalType());
                final TextColumnBuilder<BigDecimal> reservedColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.reserved"),
                            "reserved", DynamicReports.type.bigDecimalType());
                _builder.addColumn(quantityColumn, reservedColumn);
            }

            final TextColumnBuilder<BigDecimal> costColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.cost"),
                            "cost", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.total"),
                            "total", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.currency"),
                            "currency", DynamicReports.type.stringType());

            if (isEvaluateCost(_parameter)) {
                _builder.addColumn(costColumn, totalColumn, currencyColumn);
                if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    final ColumnTitleGroupBuilder costGroup = DynamicReports.grid.titleGroup(
                                    DBProperties.getProperty(InventoryReport.class.getName() + ".TitleGroup.cost"),
                                    costColumn, totalColumn, currencyColumn);
                    groupBuilders.add(costGroup);

                }
                if (TypeDisplay.GROUP.equals(getTypeDisplay(_parameter))) {
                    _builder.addSubtotalAtGroupFooter(typeGroup, DynamicReports.sbt.sum(totalColumn));
                }
                if (ClassDisplay.GROUP.equals(getClassDisplay(_parameter))) {
                    _builder.addSubtotalAtGroupFooter(classGroup, DynamicReports.sbt.sum(totalColumn));
                }
                if (StorageDisplay.ROW.equals(getStorageDisplay(_parameter))) {
                    _builder.addSubtotalAtGroupFooter(storageSumGroup, DynamicReports.sbt.sum(totalColumn));
                }
                _builder.addSubtotalAtColumnFooter(DynamicReports.sbt.sum(totalColumn));
            }
            if (!groupBuilders.isEmpty()) {
                _builder.columnGrid(groupBuilders.toArray(new ColumnTitleGroupBuilder[groupBuilders.size()]));
            }
        }

        public List<String> getStorages(final Parameter _parameter)
            throws EFapsException
        {
            final List<String> ret = new ArrayList<>();
            for (final InventoryBean bean : getBeans(_parameter, null)) {
                if (!ret.contains(bean.getStorage())) {
                    ret.add(bean.getStorage());
                }
            }
            Collections.sort(ret);
            return ret;
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

        /**
         * Getter method for the instance variable {@link #beans}.
         * @param _parameter Parameter as passed by the eFaps API
         * @param _inventory Inventory instance
         * @return value of instance variable {@link #beans}
         * @throws EFapsException on error
         */
        public List<InventoryBean> getBeans(final Parameter _parameter,
                                            final Inventory _inventory)
            throws EFapsException
        {
            if (this.beans == null) {
                final Inventory inventory;
                if (_inventory == null) {
                    inventory = getInventoryObject(_parameter);
                    inventory.setStorageInsts(getStorageInsts(_parameter));
                    inventory.setEvaluateCost(isEvaluateCost(_parameter));
                    inventory.setShowStorage(!StorageDisplay.NONE.equals(getStorageDisplay(_parameter)));
                    inventory.setShowProdClass(!ClassDisplay.NONE.equals(getClassDisplay(_parameter)));
                    inventory.setDate((DateTime) getFilterMap(_parameter).get("date"));
                } else {
                    inventory = _inventory;
                }
                this.beans = inventory.getInventory(_parameter);
            }
            return this.beans;
        }

        /**
         * Setter method for instance variable {@link #beans}.
         *
         * @param _beans value for instance variable {@link #beans}
         */
        public void setBeans(final List<InventoryBean> _beans)
        {
            this.beans = _beans;
        }
    }

    /**
     * Expression used to render a link for the UserInterface.
     */
    public static class ProductLinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Costructor.
         */
        public ProductLinkExpression()
        {
            addExpression(DynamicReports.field("prodOID", String.class));
        }

        @Override
        public EmbeddedLink evaluate(final List<?> _values,
                                     final ReportParameters _reportParameters)
        {
            final String oid = (String) _values.get(0);
            return EmbeddedLink.getJasperLink(oid);
        }
    }

}