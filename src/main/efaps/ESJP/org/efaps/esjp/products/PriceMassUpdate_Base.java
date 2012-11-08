/*
 * Copyright 2003 - 2012 The eFaps Team
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

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO description!
 *
 * @author The eFasp Team
 * @version $Id: TreeViewStructurBrowser_Base.java 7782 2012-07-16 14:51:05Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("5155df08-55a6-4ad2-ad93-374820d9fc45")
@EFapsRevision("$Rev$")
public abstract class PriceMassUpdate_Base
{
    /**
     * key used for a Request.
     */
    private final static String REQUESTKEY = "org.efaps.esjp.products.PriceMassUpdate_Base.getCurrentPriceFieldValue";

    /**
     * Get the Instances of the Products that the update of prices must be done.
     * It simulates a multiprint for the embedded table.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing a list of instances
     * @throws EFapsException on error
     */
    public Return getProductInstances(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String[] rows = Context.getThreadContext().getParameters().get("selectedRow");
        final List<Instance> instances = new ArrayList<Instance>();
        if (rows != null) {
            for (final String row : rows) {
                final Instance instance = Instance.get(row);
                if (instance.isValid()) {
                    instances.add(instance);
                }
            }
        }
        ret.put(ReturnValues.VALUES, instances);
        return ret;
    }

    /**
     * Get a String Field value that show the current Price of a Product.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing teh price for the current product
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public Return getCurrentPriceFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        Map<Instance, String> values;
        if (Context.getThreadContext().containsRequestAttribute(PriceMassUpdate_Base.REQUESTKEY)) {
            values = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(
                            PriceMassUpdate_Base.REQUESTKEY);
        } else {
            values = new HashMap<Instance, String>();
            Context.getThreadContext().setRequestAttribute(PriceMassUpdate_Base.REQUESTKEY, values);
            final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

            final DateTime date = getDate4CurrentPrice(_parameter);
            final String priceListType = String.valueOf(properties.get("PriceListType"));

            final List<Instance> productInsts = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            final List<Long> productids = new ArrayList<Long>();
            for (final Instance inst : productInsts) {
                productids.add(inst.getId());
            }

            final QueryBuilder queryBldr = new QueryBuilder(Type.get(priceListType));
            queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ProductAbstractLink, productids.toArray());
            queryBldr.addWhereAttrLessValue(CIProducts.ProductPricelistAbstract.ValidFrom, date.plusSeconds(1));
            queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistAbstract.ValidUntil, date.minusSeconds(1));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selProdOid = new SelectBuilder().linkto(
                            CIProducts.ProductPricelistAbstract.ProductAbstractLink).oid();
            multi.addSelect(selProdOid);
            multi.execute();
            while (multi.next()) {
                final Instance prodInst = Instance.get(multi.<String>getSelect(selProdOid));
                final StringBuilder valBldr = new StringBuilder();
                final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.ProductPricelistPosition);
                queryBldr2.addWhereAttrEqValue(CIProducts.ProductPricelistPosition.ProductPricelist,
                                multi.getCurrentInstance().getId());
                final MultiPrintQuery multi2 = queryBldr2.getPrint();
                final SelectBuilder selCurrSym = new SelectBuilder().linkto(
                                CIProducts.ProductPricelistPosition.CurrencyId)
                                .attribute(CIERP.Currency.Symbol);
                multi2.addAttribute(CIProducts.ProductPricelistPosition.Price);
                multi2.addSelect(selCurrSym);
                multi2.execute();
                while (multi2.next()) {
                    valBldr.append(getDigitsformater(_parameter).format(
                                    multi2.getAttribute(CIProducts.ProductPricelistPosition.Price)))
                           .append(" ")
                           .append(multi2.getSelect(selCurrSym));
                }
                values.put(prodInst, valBldr.toString());
            }
        }
        ret.put(ReturnValues.VALUES, values.get(_parameter.getInstance()));
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Decimal Format
     * @throws EFapsException on error
     */
    protected DecimalFormat getDigitsformater(final Parameter _parameter)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setMinimumFractionDigits(2);
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return a DateTime
     * @throws EFapsException on error
     */
    protected DateTime getDate4CurrentPrice(final Parameter _parameter)
        throws EFapsException
    {
        return new DateTime();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return execute(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String[] rowIds = _parameter.getParameterValues(EFapsKey.TABLEROW_NAME.getKey());
        final String[] newPrices = _parameter.getParameterValues("newPrice");
        final DecimalFormat formater = getDigitsformater(_parameter);
        @SuppressWarnings("unchecked")
        final Map<String, String> rowid2oid = (Map<String, String>) _parameter.get(ParameterValues.OIDMAP4UI);
        for (int i = 0; i< rowIds.length ; i++ ) {
            final String rowId = rowIds[i];
            final Instance inst = Instance.get(rowid2oid.get(rowId));
            if (inst.isValid() && !newPrices[i].isEmpty()) {
                try {
                    final Number newPrice = formater.parse(newPrices[i]);

                    final String priceListType = String.valueOf(properties.get("PriceListType"));

                    final Insert insert = new Insert(Type.get(priceListType));
                    insert.add(CIProducts.ProductPricelistAbstract.ProductAbstractLink, inst.getId());
                    insert.add(CIProducts.ProductPricelistAbstract.ValidFrom, getDate4ValidFrom(_parameter));
                    insert.add(CIProducts.ProductPricelistAbstract.ValidUntil, getDate4ValidUntil(_parameter));
                    insert.execute();

                    final Insert insert2 = new Insert(CIProducts.ProductPricelistPosition);
                    insert2.add(CIProducts.ProductPricelistPosition.ProductPricelist, insert.getInstance().getId());
                    insert2.add(CIProducts.ProductPricelistPosition.Price, newPrice);
                    insert2.add(CIProducts.ProductPricelistPosition.CurrencyId, getCurrencyId(_parameter, i));
                    insert2.execute();
                } catch (final ParseException e) {
                    throw new EFapsException(PriceMassUpdate.class, "invalidPrice", newPrices[i]);
                }
            }
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return id fo a currency
     * @throws EFapsException on error
     */
    protected Long getCurrencyId(final Parameter _parameter,
                                 final int _idx)
    {
        final String[] currency = _parameter.getParameterValues("currency");
        return Long.valueOf(currency[_idx]);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return a DateTime
     * @throws EFapsException on error
     */
    protected DateTime getDate4ValidFrom(final Parameter _parameter)
        throws EFapsException
    {
        return new DateTime();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return a DateTime
     * @throws EFapsException on error
     */
    protected DateTime getDate4ValidUntil(final Parameter _parameter)
        throws EFapsException
    {
        return new DateTime().plusYears(10);
    }

}
