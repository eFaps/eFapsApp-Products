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
package org.efaps.esjp.products;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CITableProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.datetime.DateAndTimeUtils;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.products.util.Products;
import org.efaps.ui.wicket.models.field.UIField;
import org.efaps.ui.wicket.models.objects.AbstractUIPageObject;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO description!
 *
 * @author The eFasp Team
 */
@EFapsUUID("5155df08-55a6-4ad2-ad93-374820d9fc45")
@EFapsApplication("eFapsApp-Products")
public abstract class PriceMassUpdate_Base
    extends AbstractCommon
{

    private static final Logger LOG = LoggerFactory.getLogger(PriceMassUpdate.class);
    /**
     * key used for a Request.
     */
    private static final String REQUESTKEY = PriceMassUpdate.class.getName() + ".RequestKey";

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return execute(final Parameter _parameter)
        throws EFapsException
    {
        final String[] rowIds = _parameter.getParameterValues("eFapsTRID");
        final String[] newPrices = _parameter.getParameterValues("newPrice");
        final String[] currencies = _parameter.getParameterValues("currency");
        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        @SuppressWarnings("unchecked") final Map<String, String> rowid2oid = (Map<String, String>) _parameter
                        .get(ParameterValues.OIDMAP4UI);
        final String priceListTypeName = getProperty(_parameter, "PriceListType");

        final List<MassUpdateEntry> entries = new ArrayList<>();
        final Instance pGroupInst = Instance.get(_parameter.getParameterValue(
                        CIFormProducts.Products_PriceMassUpdateForm.priceGroupLink.name));
        for (int i = 0; i < rowIds.length; i++) {
            final String rowId = rowIds[i];
            final Instance prodInst = Instance.get(rowid2oid.get(rowId));
            if (prodInst.isValid() && !newPrices[i].isEmpty()) {
                try {
                    final var newPrice = formater.parse(newPrices[i]);
                    final var currencyInstance = CurrencyInst.get(Long.valueOf(currencies[i])).getInstance();
                    final var entry = new MassUpdateEntry()
                                    .setProductInstance(prodInst)
                                    .setNewPrice(newPrice)
                                    .setCurrencyInstance(currencyInstance);
                    if (pGroupInst.isValid()) {
                        entry.setPriceGroupInstance(pGroupInst);
                    }
                    entries.add(entry);

                } catch (final ParseException e) {
                    LOG.error("Could not parse number in line {}", i);
                }
            }
        }
        execute(_parameter, entries, Type.get(priceListTypeName));
        return new Return();
    }

    public void execute(final Parameter parameter,
                        final Collection<MassUpdateEntry> entries,
                        final Type priceListType)
        throws EFapsException
    {
        for (final var entry : entries) {
            final Instance prodInst = entry.getProductInstance();
            if (prodInst.isValid()) {
                Instance productPricelistInst = null;
                if (Products.ACTIVATEPRICEGRP.get()) {
                    // check if we want to update (meaning edited today)
                    final QueryBuilder queryBldr = new QueryBuilder(priceListType);
                    queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ProductAbstractLink,
                                    prodInst);
                    queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ValidFrom,
                                    getDate4ValidFrom(parameter));
                    queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ValidUntil,
                                    getDate4ValidUntil(parameter));
                    final InstanceQuery query = queryBldr.getQuery();
                    query.execute();
                    if (query.next()) {
                        productPricelistInst = query.getCurrentValue();
                    }
                }
                if (InstanceUtils.isNotValid(productPricelistInst)) {
                    MultiPrintQuery multi = null;
                    if (Products.ACTIVATEPRICEGRP.get()) {
                        // carry over
                        final var date = getDate4CurrentPrice(parameter);
                        final QueryBuilder attrQueryBldr = new QueryBuilder(priceListType);
                        attrQueryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ProductAbstractLink,
                                        prodInst);
                        attrQueryBldr.addWhereAttrLessValue(CIProducts.ProductPricelistAbstract.ValidFrom,
                                        date.plusSeconds(1));
                        attrQueryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistAbstract.ValidUntil,
                                        date.minusSeconds(1));

                        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductPricelistPosition);
                        queryBldr.addWhereAttrInQuery(CIProducts.ProductPricelistPosition.ProductPricelist,
                                        attrQueryBldr.getAttributeQuery(CIProducts.ProductPricelistAbstract.ID));
                        multi = queryBldr.getPrint();
                        multi.addAttribute(CIProducts.ProductPricelistPosition.Price,
                                        CIProducts.ProductPricelistPosition.CurrencyId,
                                        CIProducts.ProductPricelistPosition.PriceGroupLink);
                        multi.execute();
                    }

                    final Insert insert = new Insert(priceListType);
                    insert.add(CIProducts.ProductPricelistAbstract.ProductAbstractLink, prodInst);
                    insert.add(CIProducts.ProductPricelistAbstract.ValidFrom, getDate4ValidFrom(parameter));
                    insert.add(CIProducts.ProductPricelistAbstract.ValidUntil, getDate4ValidUntil(parameter));
                    insert.execute();
                    productPricelistInst = insert.getInstance();
                    if (multi != null) {
                        while (multi.next()) {
                            final Insert carryOverInsert = new Insert(CIProducts.ProductPricelistPosition);
                            carryOverInsert.add(CIProducts.ProductPricelistPosition.ProductPricelist,
                                            productPricelistInst);
                            carryOverInsert.add(CIProducts.ProductPricelistPosition.PriceGroupLink,
                                            multi.<Long>getAttribute(
                                                            CIProducts.ProductPricelistPosition.PriceGroupLink));
                            carryOverInsert.add(CIProducts.ProductPricelistPosition.Price,
                                            multi.<BigDecimal>getAttribute(
                                                            CIProducts.ProductPricelistPosition.Price));
                            carryOverInsert.add(CIProducts.ProductPricelistPosition.CurrencyId,
                                            multi.<Long>getAttribute(
                                                            CIProducts.ProductPricelistPosition.CurrencyId));
                            carryOverInsert.execute();
                        }
                    }
                }

                Update update = null;
                if (Products.ACTIVATEPRICEGRP.get()) {
                    final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductPricelistPosition);
                    if (InstanceUtils.isValid(entry.getPriceGroupInstance())) {
                        queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistPosition.PriceGroupLink,
                                        entry.getPriceGroupInstance());
                    } else {
                        queryBldr.addWhereAttrIsNull(CIProducts.ProductPricelistPosition.PriceGroupLink);
                    }
                    queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistPosition.ProductPricelist,
                                    productPricelistInst);
                    final InstanceQuery query = queryBldr.getQuery();
                    query.execute();
                    if (query.next()) {
                        update = new Update(query.getCurrentValue());
                    }
                }
                if (update == null) {
                    update = new Insert(CIProducts.ProductPricelistPosition);
                    update.add(CIProducts.ProductPricelistPosition.ProductPricelist, productPricelistInst);
                    if (InstanceUtils.isValid(entry.getPriceGroupInstance())) {
                        update.add(CIProducts.ProductPricelistPosition.PriceGroupLink, entry.getPriceGroupInstance());
                    }
                }
                update.add(CIProducts.ProductPricelistPosition.Price, entry.getNewPrice());
                update.add(CIProducts.ProductPricelistPosition.CurrencyId, entry.getCurrencyInstance());
                update.execute();
            }
        }
    }

    /**
     * Field update for price group.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return fieldUpdate4PriceGroup(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String[] rowKeys = _parameter.getParameterValues("eFapsTRID");
        final Map<String, String> mapping = ((AbstractUIPageObject) ((UIField) _parameter.get(ParameterValues.CLASS))
                        .getParent()).getUiID2Oid();
        final List<Instance> instances = new ArrayList<>();
        for (final String rowKey : rowKeys) {
            instances.add(Instance.get(mapping.get(rowKey)));
        }
        final Collection<Map<String, Object>> valueList = new ArrayList<>();
        final Map<Instance, String> values = getValues(_parameter, instances);
        for (final Instance inst : instances) {
            final Map<String, Object> map = new HashMap<>();
            valueList.add(map);
            map.put(CITableProducts.Products_PriceMassUpdateTable.currentPrice.name,
                            values.containsKey(inst) ? values.get(inst) : "");
        }
        ret.put(ReturnValues.VALUES, valueList);
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
        final Map<Instance, String> values;
        if (Context.getThreadContext().containsRequestAttribute(PriceMassUpdate_Base.REQUESTKEY)) {
            values = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(
                            PriceMassUpdate_Base.REQUESTKEY);
        } else {
            final List<Instance> productInsts = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            values = getValues(_parameter, productInsts);
            Context.getThreadContext().setRequestAttribute(PriceMassUpdate_Base.REQUESTKEY, values);
        }
        ret.put(ReturnValues.VALUES, values.get(_parameter.getInstance()));
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return a DateTime
     * @throws EFapsException on error
     */
    protected OffsetDateTime getDate4CurrentPrice(final Parameter _parameter)
        throws EFapsException
    {
        return DateAndTimeUtils.withTimeAtStartOfDay();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return a DateTime
     * @throws EFapsException on error
     */
    protected LocalDate getDate4ValidFrom(final Parameter _parameter)
        throws EFapsException
    {
        return LocalDate.now(Context.getThreadContext().getZoneId());
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return a DateTime
     * @throws EFapsException on error
     */
    protected LocalDate getDate4ValidUntil(final Parameter _parameter)
        throws EFapsException
    {
        return LocalDate.now(Context.getThreadContext().getZoneId()).plusYears(10);
    }

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
        // get it form the context
        final String[] rows = Context.getThreadContext().getParameters().get("selectedRow");
        final List<Instance> instances = new ArrayList<>();
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
     * Gets the values.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _productInsts the product insts
     * @return the values
     * @throws EFapsException on error
     */
    protected Map<Instance, String> getValues(final Parameter _parameter,
                                              final List<Instance> _productInsts)
        throws EFapsException
    {
        final Map<Instance, String> ret = new HashMap<>();
        final var date = getDate4CurrentPrice(_parameter);
        final String priceListType = getProperty(_parameter, "PriceListType");

        final QueryBuilder queryBldr = new QueryBuilder(Type.get(priceListType));
        queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ProductAbstractLink, _productInsts.toArray());
        queryBldr.addWhereAttrLessValue(CIProducts.ProductPricelistAbstract.ValidFrom, date.plusSeconds(1));
        queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistAbstract.ValidUntil, date.minusSeconds(1));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.setEnforceSorted(true);
        final SelectBuilder selProdOid = new SelectBuilder().linkto(
                        CIProducts.ProductPricelistAbstract.ProductAbstractLink).oid();
        multi.addSelect(selProdOid);
        multi.execute();
        final DecimalFormat formatter = NumberFormatter.get().getTwoDigitsFormatter();
        while (multi.next()) {
            final Instance prodInst = Instance.get(multi.<String>getSelect(selProdOid));
            final StringBuilder valBldr = new StringBuilder();
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.ProductPricelistPosition);
            queryBldr2.addWhereAttrEqValue(CIProducts.ProductPricelistPosition.ProductPricelist,
                            multi.getCurrentInstance());
            if (Products.ACTIVATEPRICEGRP.get()) {
                final Instance pGroupInts = Instance.get(_parameter.getParameterValue(
                                CIFormProducts.Products_PriceMassUpdateForm.priceGroupLink.name));
                if (pGroupInts.isValid()) {
                    queryBldr2.addWhereAttrEqValue(CIProducts.ProductPricelistPosition.PriceGroupLink, pGroupInts);
                } else {
                    queryBldr2.addWhereAttrIsNull(CIProducts.ProductPricelistPosition.PriceGroupLink);
                }
            }
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            final SelectBuilder selCurrSym = new SelectBuilder().linkto(CIProducts.ProductPricelistPosition.CurrencyId)
                            .attribute(CIERP.Currency.Symbol);
            multi2.addAttribute(CIProducts.ProductPricelistPosition.Price);
            multi2.addSelect(selCurrSym);
            multi2.execute();
            while (multi2.next()) {
                valBldr.append(formatter.format(multi2.getAttribute(CIProducts.ProductPricelistPosition.Price)))
                                .append(" ").append(multi2.<String>getSelect(selCurrSym));
            }
            ret.put(prodInst, valBldr.toString());
        }
        return ret;
    }

    public static class MassUpdateEntry
    {

        private Instance productInstance;
        private Number newPrice;
        private Instance currencyInstance;
        private Instance priceGroupInstance;

        public Instance getCurrencyInstance()
        {
            return currencyInstance;
        }

        public Number getNewPrice()
        {
            return newPrice;
        }

        public Instance getPriceGroupInstance()
        {
            return priceGroupInstance;
        }

        public Instance getProductInstance()
        {
            return productInstance;
        }

        public MassUpdateEntry setCurrencyInstance(final Instance currencyInstance)
        {
            this.currencyInstance = currencyInstance;
            return this;
        }

        public MassUpdateEntry setNewPrice(final Number newPrice)
        {
            this.newPrice = newPrice;
            return this;
        }

        public MassUpdateEntry setPriceGroupInstance(final Instance priceGroupInstance)
        {
            this.priceGroupInstance = priceGroupInstance;
            return this;
        }

        public MassUpdateEntry setProductInstance(final Instance productInstance)
        {
            this.productInstance = productInstance;
            return this;
        }
    }
}
