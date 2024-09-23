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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.api.ui.IFilterList;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.products.listener.IPriceListListener;
import org.efaps.esjp.products.util.Products;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("ccbcbc48-1c82-42f4-8e31-2eb496e862c1")
@EFapsApplication("eFapsApp-Products")
public abstract class PriceList_Base
    extends MultiPrint
{

    /**
     * Method to get the value for the date field "Valid until". On create mode
     * a date in ten years future is returned.
     *
     * @param _parameter Paramter as passed from the eFaps esjp API
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return getValidUntilUI(final Parameter _parameter)
        throws EFapsException
    {
        final IUIValue fValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final DateTime value;
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            value = new DateTime().plusYears(10);
        } else {
            value = (DateTime) fValue.getObject();
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, value);
        return ret;
    }

    /**
     * Method is executed as a insert trigger on type
     * "Products_ProductPricelistAbstract" and children. It corrects the valid
     * until date of all other Instances.
     *
     * @param _parameter Parameter as passed from the eFaps esjp API
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return trigger4Insert(final Parameter _parameter)
        throws EFapsException
    {
        final Instance priceListInstance = _parameter.getInstance();

        final var eval = EQL.builder().print(priceListInstance)
            .attribute(CIProducts.ProductPricelistAbstract.ValidFrom)
            .evaluate();
        eval.next();

        final LocalDate validFrom = eval.get(CIProducts.ProductPricelistAbstract.ValidFrom);

        final QueryBuilder queryBldr = new QueryBuilder(eval.inst().getType());
        queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistAbstract.ValidUntil, validFrom);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ProductAbstractLink,
                        getProdId(priceListInstance));
        add2QueryBuilder4TriggerInsert(_parameter, queryBldr);
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final List<String> updateOIDs = new ArrayList<>();
        while (query.next()) {
            final String oid = query.getCurrentValue().getOid();
            if (!priceListInstance.getOid().equals(oid)) {
                updateOIDs.add(oid);
            }
        }

        for (final String oid : updateOIDs) {
            final Update update = new Update(oid);
            update.add(CIProducts.ProductPricelistAbstract.ValidUntil, validFrom.minusDays(1));
            update.executeWithoutTrigger();
        }
        return new Return();
    }

    /**
     * To be used by implementation.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _queryBldr QueryBuilder to be added to
     * @throws EFapsException on error
     */
    protected void add2QueryBuilder4TriggerInsert(final Parameter _parameter,
                                                  final QueryBuilder _queryBldr)
        throws EFapsException
    {

    }

    /**
     * Method to get the id of the related product. Needed as a criteria for the
     * queries.
     *
     * @param _instance Instance to search for
     * @return id of the product
     * @throws EFapsException on error
     */
    private Long getProdId(final Instance _instance)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CIProducts.ProductPricelistAbstract.ProductAbstractLink);
        print.execute();
        return print.<Long>getAttribute(CIProducts.ProductPricelistAbstract.ProductAbstractLink);
    }

    /**
     * @param _parameter    Parameter as passed from the eFaps API
     * @return  list of instance
     * @throws EFapsException on error
     */
    public Return getPriceListPositionInstances(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Instance> instances = new ArrayList<>();

        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Map<?, ?> filter = (Map<?, ?>) _parameter.get(ParameterValues.OTHERS);

        final String types = (String) properties.get("Types");
        for (final String typeStr : types.split(";")) {
            final Type type = Type.get(typeStr);
            final QueryBuilder queryBldr = new QueryBuilder(type);
            analyzeTable(_parameter, (IFilterList) filter, queryBldr, type);
            final AttributeQuery attrQuery = queryBldr.getAttributeQuery(CIProducts.ProductPricelistAbstract.ID);

            final QueryBuilder posQueryBldr = new QueryBuilder(CIProducts.ProductPricelistPosition);
            posQueryBldr.addWhereAttrInQuery(CIProducts.ProductPricelistPosition.ProductPricelist, attrQuery);
            final InstanceQuery query = posQueryBldr.getQuery();
            instances.addAll(query.execute());
        }
        ret.put(ReturnValues.VALUES, instances);
        return ret;
    }

    /**
     * Gets the date from product pricelist retail.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the date from product pricelist retail
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public Return getDateFromProductPricelistRetail(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = fieldValue.getField().getName();

        final Map<Instance, String> listProducts;
        if (Context.getThreadContext().containsRequestAttribute(fieldName)) {
            listProducts = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(fieldName);
        } else {
            listProducts = new HashMap<>();
            Context.getThreadContext().setRequestAttribute(fieldName, listProducts);

            final List<Instance> products = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);

            final MultiPrintQuery multi = new MultiPrintQuery(products);
            multi.execute();
            while (multi.next()) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductPricelistRetail);
                queryBldr.addWhereAttrLessValue(CIProducts.ProductPricelistRetail.ValidFrom,
                                new DateTime().plusMinutes(1));
                queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistRetail.ValidUntil,
                                new DateTime().minusMinutes(1));
                queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistRetail.ProductLink, multi.getCurrentInstance()
                                .getId());
                final MultiPrintQuery mult = queryBldr.getPrint();
                mult.addAttribute("ValidFrom", "ValidUntil");
                mult.execute();
                if (mult.next()) {
                    final DateTime validFrom = mult.<DateTime>getAttribute("ValidFrom");
                    final DateTime validUntil = mult.<DateTime>getAttribute("ValidUntil");
                    String dateRange = "";
                    if (validFrom != null && validUntil != null) {
                        final String fromDateStr = validFrom.toString(DateTimeFormat.forStyle("S-")
                                        .withLocale(Context.getThreadContext().getLocale()));
                        final String untilDateStr = validUntil.toString(DateTimeFormat.forStyle("S-")
                                        .withLocale(Context.getThreadContext().getLocale()));
                        dateRange = fromDateStr.concat(" - ").concat(untilDateStr);

                        if (fieldName.equalsIgnoreCase("dateRange")) {
                            listProducts.put(multi.getCurrentInstance(), dateRange);
                        } else if (fieldName.equalsIgnoreCase("validFrom")) {
                            listProducts.put(multi.getCurrentInstance(), fromDateStr);
                        } else {
                            listProducts.put(multi.getCurrentInstance(), untilDateStr);
                        }
                    }
                }
            }
        }

        ret.put(ReturnValues.VALUES, listProducts.get(_parameter.getInstance()));
        return ret;
    }

    /**
     * Gets the price from product pricelist.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the price from product pricelist
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public Return getPriceFromProductPricelist(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = fieldValue.getField().getName();

        final Map<Instance, String> listProducts2Price;
        if (Context.getThreadContext().containsRequestAttribute(fieldName)) {
            listProducts2Price = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(fieldName);
        } else {
            listProducts2Price = new HashMap<>();
            Context.getThreadContext().setRequestAttribute(fieldName, listProducts2Price);

            final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final String typeStr = (String) properties.get("Type");
            final Type type = Type.get(typeStr);

            final List<Instance> products = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            final MultiPrintQuery multi = new MultiPrintQuery(products);
            multi.execute();
            while (multi.next()) {
                final QueryBuilder queryBldr = new QueryBuilder(type);
                queryBldr.addWhereAttrLessValue(CIProducts.ProductPricelistAbstract.ValidFrom,
                                new DateTime().plusMinutes(1));
                queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistAbstract.ValidUntil,
                                new DateTime().minusMinutes(1));
                queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ProductAbstractLink, multi
                                .getCurrentInstance()
                                .getId());
                final AttributeQuery attrQuery = queryBldr.getAttributeQuery(CIProducts.ProductPricelistAbstract.ID);

                final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.ProductPricelistPosition);
                queryBldr2.addWhereAttrInQuery(CIProducts.ProductPricelistPosition.ProductPricelist, attrQuery);
                final MultiPrintQuery mult = queryBldr2.getPrint();
                final SelectBuilder selCurrencyId = new SelectBuilder().linkto(
                                CIProducts.ProductPricelistPosition.CurrencyId)
                                .attribute("Name");
                mult.addSelect(selCurrencyId);
                mult.addAttribute("Price");
                mult.execute();
                if (mult.next()) {
                    final BigDecimal price = mult.<BigDecimal>getAttribute("Price");
                    final String currencyName = mult.<String>getSelect(selCurrencyId);
                    final String priceStr = price.toString();

                    if (fieldName.contains("price") && priceStr != null) {
                        listProducts2Price.put(multi.getCurrentInstance(), priceStr);
                    }

                    if (fieldName.contains("currencyId") && currencyName != null) {
                        listProducts2Price.put(multi.getCurrentInstance(), currencyName);
                    }
                }
            }
        }

        ret.put(ReturnValues.VALUES, listProducts2Price.get(_parameter.getInstance()));
        return ret;
    }

    /**
     * Check if a given group applies.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _priceGroupInstance the price group instance
     * @return true, if successful
     * @throws EFapsException on error
     */
    public boolean groupApplies(final Parameter _parameter,
                                final Instance _priceGroupInstance)
        throws EFapsException
    {
        boolean ret = false;
        if (Products.ACTIVATEPRICEGRP.get() && InstanceUtils.isValid(_priceGroupInstance)) {
            if (InstanceUtils.isType(_priceGroupInstance, CIProducts.PriceGroupContact)) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.PriceGroupContact2Contact);
                queryBldr.addWhereAttrEqValue(CIProducts.PriceGroupContact2Contact.FromLink, _priceGroupInstance);
                final Instance contactInst = getContactInstance(_parameter);
                if (contactInst.isValid()) {
                    queryBldr.addWhereAttrEqValue(CIProducts.PriceGroupContact2Contact.ToLink, contactInst);
                    ret = !queryBldr.getQuery().execute().isEmpty();
                }
            }
            if (!ret) {
                for (final IPriceListListener listener : Listener.get()
                                .<IPriceListListener>invoke(IPriceListListener.class)) {
                    ret = listener.groupApplies(_parameter, _priceGroupInstance);
                    if (ret) {
                        break;
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Gets the contact instance.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the contact instance
     * @throws EFapsException on error
     */
    protected Instance getContactInstance(final Parameter _parameter)
        throws EFapsException
    {
        final String contactStr = _parameter.getParameterValue("contact");
        return Instance.get(contactStr);
    }
}
