/*
 * Copyright 2003 - 2011 The eFaps Team
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


package org.efaps.esjp.products;

import java.math.BigDecimal;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("3ee86b77-a8b1-4ec5-bb11-2d2071694344")
@EFapsApplication("eFapsApp-Products")
public abstract class CalculateInventoryValueSource_Base
    extends  CalculateInventorySource
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        super.init(_jasperReport, _parameter, _parentSource, _jrParameters);
        final String dateStr = _parameter.getParameterValue("date");
        // get a new DateTime from the ISO Date String from the Parameters
        final DateTime date = new DateTime(dateStr);
        for (final Map<String, Object> value: getValues()) {
            final Instance prodInst = Instance.get((String) value.get("ProductOid"));
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductCost);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink, prodInst.getId());
            queryBldr.addWhereAttrGreaterValue(CIProducts.ProductCost.ValidUntil, date.minusMinutes(1));
            queryBldr.addWhereAttrLessValue(CIProducts.ProductCost.ValidFrom, date.plusMinutes(1));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder sel = new SelectBuilder().linkto(CIProducts.ProductCost.CurrencyLink)
                .attribute(CIERP.Currency.Symbol);
            multi.addSelect(sel);
            multi.addAttribute(CIProducts.ProductCost.Price);
            multi.execute();
            if (multi.next()) {
                value.put("Price", multi.<BigDecimal>getAttribute(CIProducts.ProductCost.Price));
                value.put("Currency", multi.<String>getSelect(sel));
            }
        }
    }
}
