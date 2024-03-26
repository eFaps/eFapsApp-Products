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
package org.efaps.esjp.products.reports;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.products.BOMCalculator;
import org.efaps.util.EFapsException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

/**
 * The Class TerminateProductReport_Base.
 */
@EFapsUUID("5e19ad42-5fc1-43ff-9cc5-5b1fe5f5105f")
@EFapsApplication("eFapsApp-Products")
public abstract class TerminateProductReport_Base
{

    /**
     * Gets the report.
     *
     * @param _parameter the parameter
     * @return the report
     * @throws EFapsException the e faps exception
     */
    public Return getReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = new ProductReport();
        dyRp.setFileName(DBProperties.getProperty(TerminateProductReport.class.getName() + ".FileName"));
        final String html = dyRp.getHtml(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * Export report.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = new ProductReport();
        dyRp.setFileName(DBProperties.getProperty(TerminateProductReport.class.getName() + ".FileName"));
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
     * The Class ProductReport.
     */
    public static class ProductReport
        extends AbstractDynamicReport
    {

        /* (non-Javadoc)
         * @see org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base#createDataSource(org.efaps.admin.event.Parameter)
         */
        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource ret = new DRDataSource("name", "description", "quantity");
            final Instance instance = _parameter.getInstance();
            new org.efaps.esjp.products.Product();

            final PrintQuery print = new PrintQuery(instance);
            print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description);
            print.execute();
            final String nameProduct = print.<String>getAttribute(CIProducts.ProductAbstract.Name);
            final String description = print.<String>getAttribute(CIProducts.ProductAbstract.Description);

            final BOMCalculator bomCalc = new BOMCalculator(_parameter, instance, null);

            ret.add(nameProduct, description, bomCalc.getQuantityOnPaper());

            return ret;
        }

        /* (non-Javadoc)
         * @see org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base#addColumnDefinition(org.efaps.admin.event.Parameter, net.sf.dynamicreports.jasper.builder.JasperReportBuilder)
         */
        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            final TextColumnBuilder<String> nameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(TerminateProductReport.class.getName() + ".Column.name"),
                            "name", DynamicReports.type.stringType());
            final TextColumnBuilder<String> descriptionColumn = DynamicReports.col.column(DBProperties
                            .getProperty(TerminateProductReport.class.getName() + ".Column.description"),
                            "description", DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty(TerminateProductReport.class.getName() + ".Column.quantity"),
                            "quantity", DynamicReports.type.bigDecimalType());

            _builder.addColumn(nameColumn, descriptionColumn, quantityColumn);
        }
    }

}
