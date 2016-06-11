/*
 * Copyright 2003 - 2015 The eFaps Team
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
 */

package org.efaps.esjp.products;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.efaps.admin.access.AccessTypeEnums;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CITableProducts;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.WarningUtil;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("aa16287e-6148-41d2-a40f-05a65946ecfc")
@EFapsApplication("eFapsApp-Products")
public abstract class Transaction_Base
    extends CommonDocument
{

    /**
     * Key used to store info during request.
     */
    public static final String REQUESTKEY4TRANSDATEPR = Transaction_Base.class + ".RequestKey4TransactionDate";

    /**
     * Key used to store info during request.
     */
    public static final String REQUESTKEY = Transaction_Base.class + ".RequestKey";

    /**
     * Key used to store info during request.
     */
    public static final String STORAGEINSTKEY = Transaction_Base.class + ".StorageInstKey";

    /**
     * Used to store the Revision in the Context.
     */
    public static final String NAMEKEY = Transaction.class.getName() + ".NameKey";

    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Transaction.class);

    /**
     * Method to assign the signum for the quantity value.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return getQuantityFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue fieldvalue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final BigDecimal value = (BigDecimal) fieldvalue.getObject();
        if (value != null) {
            if (fieldvalue.getInstance().getType().isKindOf(CIProducts.TransactionOutbound.getType()) || fieldvalue
                            .getInstance().getType().isKindOf(CIProducts.TransactionIndividualOutbound.getType())
                            || fieldvalue.getInstance().getType().isKindOf(CIProducts.TransactionReservationOutbound
                                            .getType())) {
                final BigDecimal retValue = value.negate();
                ret.put(ReturnValues.VALUES, retValue);
            }
        }
        return ret;
    }

    /**
     * Method to calculate the StockTotal for Costing.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    @SuppressWarnings("unchecked")
    public Return getStockTotalFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        Map<Instance, BigDecimal> values;
        if (Context.getThreadContext().containsRequestAttribute(Transaction_Base.REQUESTKEY)) {
            values = (Map<Instance, BigDecimal>) Context.getThreadContext().getRequestAttribute(
                            Transaction_Base.REQUESTKEY);
        } else {
            values = new HashMap<Instance, BigDecimal>();
            Context.getThreadContext().setRequestAttribute(Transaction_Base.REQUESTKEY, values);
            final List<Instance> costingInsts = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);

            final MultiPrintQuery multi = new MultiPrintQuery(costingInsts);
            final SelectBuilder selQty = SelectBuilder.get().linkto(CIProducts.Costing.TransactionAbstractLink)
                            .attribute(CIProducts.TransactionAbstract.Quantity);
            final SelectBuilder selInst = SelectBuilder.get().linkto(CIProducts.Costing.TransactionAbstractLink)
                            .instance();
            multi.addSelect(selInst, selQty);
            multi.addAttribute(CIProducts.Costing.Quantity);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final BigDecimal costQty = multi.<BigDecimal>getAttribute(CIProducts.Costing.Quantity);
                final Instance inst = multi.<Instance>getSelect(selInst);
                BigDecimal transQty = multi.<BigDecimal>getSelect(selQty);
                if (inst.getType().isKindOf(CIProducts.TransactionOutbound.getType())) {
                    transQty = transQty.negate();
                }
                values.put(multi.getCurrentInstance(), costQty.add(transQty));
            }
        }
        ret.put(ReturnValues.VALUES, values.get(_parameter.getInstance()));
        return ret;
    }

    /**
     * Method to create a Transaction manually.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Type docType = getType4DocCreate(_parameter);
        if (docType.isKindOf(CIProducts.TransactionIndividualAbstract)
                        && Boolean.parseBoolean(_parameter.getParameterValue(
                                        CIFormProducts.Products_TransactionIndividualForm.inout.name))) {

            final Instance productInst = Instance.get(_parameter.getParameterValue(getFieldName4Attribute(_parameter,
                            CIFormProducts.Products_TransactionInOutForm.product.name)));
            final PrintQuery print = new PrintQuery(productInst);
            final SelectBuilder selProdInst = SelectBuilder.get()
                            .linkfrom(CIProducts.StockProductAbstract2IndividualAbstract.ToAbstract)
                            .linkto(CIProducts.StockProductAbstract2IndividualAbstract.FromAbstract).instance();
            print.addSelect(selProdInst);
            print.executeWithoutAccessCheck();
            final Instance stockProdIns = print.getSelect(selProdInst);
            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setParmeterValue(parameter, getFieldName4Attribute(_parameter,
                            CIFormProducts.Products_TransactionInOutForm.product.name), stockProdIns.getOid());
            final CreatedDoc inoutDoc;
            if (docType.isCIType(CIProducts.TransactionIndividualInbound)) {
                inoutDoc = createDoc(parameter, CIProducts.TransactionInbound.getType());
            } else {
                inoutDoc = createDoc(parameter,  CIProducts.TransactionOutbound.getType());
            }
            final CreatedDoc transDoc = createDocumentTransaction(_parameter, inoutDoc);
            final CreatedDoc createDoc = createDoc(_parameter, docType);

            if (transDoc.getInstance().isValid()) {
                final Update update = new Update(createDoc.getInstance());
                update.add(CIProducts.TransactionAbstract.Document, transDoc.getInstance().getId());
                update.executeWithoutTrigger();
            }

        } else {
            final CreatedDoc createDoc = createDoc(_parameter, docType);
            createDocumentTransaction(_parameter, createDoc);
        }
        return new Return();
    }

    /**
     * Creates the document transaction.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createDoc the _create doc
     * @return the created doc
     * @throws EFapsException on error
     */
    protected CreatedDoc createDocumentTransaction(final Parameter _parameter,
                                                   final CreatedDoc _createDoc)
        throws EFapsException
    {
        CreatedDoc ret = null;
        final String productDocumentType = _parameter.getParameterValue("productDocumentType");
        if (productDocumentType != null) {
            final Instance prodDocInst = Instance.get(productDocumentType);
            if (prodDocInst.isValid() && (_createDoc.getInstance().getType().isCIType(CIProducts.TransactionInbound)
                            || _createDoc.getInstance().getType().isCIType(CIProducts.TransactionOutbound))) {
                ret = new TransactionDocument().createDoc(_parameter, _createDoc);
                if (ret.getInstance().isValid()) {
                    // Sales_Document2ProductDocumentType
                    final Insert insert = new Insert(UUID.fromString("29438fb0-8b1f-4e4e-a409-812b2f9efdc0"));
                    insert.add("DocumentLink", ret.getInstance().getId());
                    insert.add("DocumentTypeLink", prodDocInst.getId());
                    insert.execute();

                    final Update update = new Update(_createDoc.getInstance());
                    update.add(CIProducts.TransactionAbstract.Document, ret.getInstance().getId());
                    update.executeWithoutTrigger();
                }
            }
        }
        return ret;
    }

    /**
     * Creates the doc.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docType the _doc type
     * @return the created doc
     * @throws EFapsException on error
     */
    protected CreatedDoc createDoc(final Parameter _parameter,
                                   final Type _docType)
        throws EFapsException
    {
        final CreatedDoc createdDoc = new CreatedDoc();

        final Insert insert = new Insert(_docType);

        final String quantity = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CIFormProducts.Products_TransactionAbstractForm.quantity.name));
        if (quantity != null) {
            insert.add(CIProducts.TransactionAbstract.Quantity, quantity);
            createdDoc.getValues().put(CIProducts.TransactionAbstract.Quantity.name, quantity);
        }

        final String uoM = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CIFormProducts.Products_TransactionAbstractForm.uoM.name));
        if (uoM != null) {
            insert.add(CIProducts.TransactionAbstract.UoM, uoM);
            createdDoc.getValues().put(CIProducts.TransactionAbstract.UoM.name, uoM);
        }

        final String storage = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CIFormProducts.Products_TransactionInOutForm.storage.name));
        if (storage != null) {
            insert.add(CIProducts.TransactionAbstract.Storage, storage);
            createdDoc.getValues().put(CIProducts.TransactionAbstract.Storage.name, storage);
        }

        final String product = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CIFormProducts.Products_TransactionInOutForm.product.name));
        if (product != null) {
            final Instance prodInst = Instance.get(product);
            if (prodInst.isValid()) {
                insert.add(CIProducts.TransactionAbstract.Product, prodInst.getId());
                createdDoc.getValues().put(CIProducts.TransactionAbstract.Product.name, prodInst.getId());
            }
        }

        final String description = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CIFormProducts.Products_TransactionAbstractForm.description.name));
        if (description != null) {
            insert.add(CIProducts.TransactionAbstract.Description, description);
            createdDoc.getValues().put(CIProducts.TransactionAbstract.Description.name, description);
        }

        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CIFormProducts.Products_TransactionAbstractForm.date.name));
        if (date != null) {
            insert.add(CIProducts.TransactionAbstract.Date, date);
            createdDoc.getValues().put(CIProducts.TransactionAbstract.Date.name, date);
        }

        add2DocCreate(_parameter, insert, createdDoc);
        insert.execute();

        createdDoc.setInstance(insert.getInstance());
        return createdDoc;
    }

    @Override
    protected Type getType4DocCreate(final Parameter _parameter)
        throws EFapsException
    {
        Type type = null;
        final String typeStr = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CIFormProducts.Products_TransactionInOutForm.type.name));
        if (typeStr != null) {
            type = Type.get(Long.parseLong(typeStr));
        }
        return type;
    }

    /**
     * Move position number.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return movePositionNumber(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (HashMap<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final boolean up = "true".equalsIgnoreCase((String) properties.get("moveUp"));
        final String[] oids = (String[]) _parameter.get(ParameterValues.OTHERS);

        if (oids != null) {
            final Instance transInst = Instance.get(oids[0]);
            // manuall access check because operation is executed withou triggers!
            if (transInst.isValid() && transInst.getType().hasAccess(transInst,
                            AccessTypeEnums.MODIFY.getAccessType())) {
                final PrintQuery print = new PrintQuery(transInst);
                print.addAttribute(CIProducts.TransactionAbstract.Product, CIProducts.TransactionAbstract.Date,
                                CIProducts.TransactionAbstract.Position);
                print.executeWithoutAccessCheck();
                final DateTime date = print.<DateTime>getAttribute(CIProducts.TransactionAbstract.Date);
                final Long prodId = print.<Long>getAttribute(CIProducts.TransactionAbstract.Product);
                final Integer pos = print.<Integer>getAttribute(CIProducts.TransactionAbstract.Position);

                final DateTime startDate = date.withTimeAtStartOfDay().minusSeconds(1);
                final DateTime endDate = date.withTimeAtStartOfDay().plusDays(1);

                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, prodId);
                queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionAbstract.Date, startDate);
                queryBldr.addWhereAttrLessValue(CIProducts.TransactionAbstract.Date, endDate);
                queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Position, up ? pos - 1 : pos + 1);
                final InstanceQuery query = queryBldr.getQuery();
                query.executeWithoutAccessCheck();
                if (query.next()) {
                    final Update update = new Update(query.getCurrentValue());
                    update.add(CIProducts.TransactionAbstract.Position, pos);
                    update.executeWithoutTrigger();

                    final Update update2 = new Update(transInst);
                    update2.add(CIProducts.TransactionAbstract.Position, up ? pos - 1 : pos + 1);
                    update2.executeWithoutTrigger();
                }
            }
        }
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionInbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return inboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromInventory(_parameter);
        setPositionNumber(_parameter);
        return new Return();
    }

    /**
     * Method is executed as trigger after the update of an
     * Products_TransactionOutbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return inboundUpdatePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        storeDateProduct4Trigger(_parameter);
        return new Return();
    }

    /**
     * Inbound update post trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return inboundUpdatePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        updatePositionNumbers(_parameter, true);
        return new Return();
    }

    /**
     * Inbound delete pre trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return inboundDeletePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        storeDateProduct4Trigger(_parameter);
        return new Return();
    }

    /**
     * Inbound delete post trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return inboundDeletePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        updatePositionNumbers(_parameter, false);
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionOutbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return outboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromInventory(_parameter);
        setPositionNumber(_parameter);
        return new Return();
    }

    /**
     * Method is executed as trigger after the update of an
     * Products_TransactionOutbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return outboundUpdatePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        storeDateProduct4Trigger(_parameter);
        return new Return();
    }

    /**
     * Outbound update post trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return outboundUpdatePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        updatePositionNumbers(_parameter, true);
        return new Return();
    }

    /**
     * Outbound delete pre trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return outboundDeletePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        storeDateProduct4Trigger(_parameter);
        return new Return();
    }

    /**
     * Outbound delete post trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return outboundDeletePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        updatePositionNumbers(_parameter, false);
        return new Return();
    }

    /**
     * Post update of a transaction, the related costing will be marked
     * as not "UpToDate".
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on errro
     */
    public Return updatePostTrigger4Costing(final Parameter _parameter)
        throws EFapsException
    {
        // search the related costing instance (only if it is still "UpToDate")
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.CostingAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.UpToDate, true);
        queryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.TransactionAbstractLink, _parameter.getInstance());
        final InstanceQuery query = queryBldr.getQuery();
        query.executeWithoutAccessCheck();
        if (query.next()) {
            final Update update = new Update(query.getCurrentValue());
            update.add(CIProducts.CostingAbstract.UpToDate, false);
            update.executeWithoutTrigger();
        }
        return new Return();
    }


    /**
     * Store date product4 trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @throws EFapsException on error
     */
    protected void storeDateProduct4Trigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(instance);
        print.addAttribute(CIProducts.TransactionAbstract.Product,
                        CIProducts.TransactionAbstract.Date);
        print.executeWithoutAccessCheck();

        Context.getThreadContext().setRequestAttribute(
                        Transaction_Base.REQUESTKEY4TRANSDATEPR,
                        new TransDateProd(print.<DateTime>getAttribute(CIProducts.TransactionAbstract.Date),
                                        print.<Long>getAttribute(CIProducts.TransactionAbstract.Product)));
    }

    /**
     * Sets the position number.
     *
     * @param _parameter    Parameter as passed by the eFaps API
     * @throws EFapsException on error
     */
    protected void setPositionNumber(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(instance);
        print.addAttribute(CIProducts.TransactionAbstract.Product,
                        CIProducts.TransactionAbstract.Date);
        print.executeWithoutAccessCheck();

        final DateTime date = print.<DateTime>getAttribute(CIProducts.TransactionAbstract.Date);
        final Long prodId = print.<Long>getAttribute(CIProducts.TransactionAbstract.Product);
        updatePositionNumbers(_parameter, date, prodId, instance);
    }

    /**
     * Update position numbers.
     *
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _current      update current instance also
     * @throws EFapsException on error
     */
    protected void updatePositionNumbers(final Parameter _parameter,
                                         final boolean _current)
        throws EFapsException
    {
        final TransDateProd transDateProd = (TransDateProd) Context.getThreadContext().getRequestAttribute(
                        Transaction_Base.REQUESTKEY4TRANSDATEPR);
        if (transDateProd != null) {
            updatePositionNumbers(_parameter, transDateProd.getDate(), transDateProd.getProdId(), null);
        }
        if (_current) {
            final Instance instance = _parameter.getInstance();
            final PrintQuery print = new PrintQuery(instance);
            print.addAttribute(CIProducts.TransactionAbstract.Product,
                            CIProducts.TransactionAbstract.Date);
            print.executeWithoutAccessCheck();

            final DateTime date = print.<DateTime>getAttribute(CIProducts.TransactionAbstract.Date);
            final Long prodId = print.<Long>getAttribute(CIProducts.TransactionAbstract.Product);
            if (transDateProd == null || !transDateProd.getDate().isEqual(date)) {
                updatePositionNumbers(_parameter, date, prodId, null);
            }
        }
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _date         date of the transaction to be updated
     * @param _prodId       id of the product the transactions belong to
     * @param _instance     instance
     * @throws EFapsException on error
     */
    protected void updatePositionNumbers(final Parameter _parameter,
                                         final DateTime _date,
                                         final Long _prodId,
                                         final Instance _instance)
        throws EFapsException
    {
        final DateTime startDate = _date.withTimeAtStartOfDay().minusSeconds(1);
        final DateTime endDate = _date.withTimeAtStartOfDay().plusDays(1);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, _prodId);
        queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionAbstract.Date, startDate);
        queryBldr.addWhereAttrLessValue(CIProducts.TransactionAbstract.Date, endDate);

        if (_instance == null) {
            int i = 1;
            queryBldr.addOrderByAttributeAsc(CIProducts.TransactionAbstract.Position);
            final InstanceQuery query = queryBldr.getQuery();
            query.executeWithoutAccessCheck();
            while (query.next()) {
                final Update update = new Update(query.getCurrentValue());
                update.add(CIProducts.TransactionAbstract.Position, i);
                update.executeWithoutTrigger();
                i++;
            }
        } else {
            queryBldr.addOrderByAttributeDesc(CIProducts.TransactionAbstract.Position);

            final InstanceQuery query = queryBldr.getQuery();
            query.setLimit(1);
            final MultiPrintQuery multi = new MultiPrintQuery(query.executeWithoutAccessCheck());
            multi.addAttribute(CIProducts.TransactionAbstract.Position);
            multi.setEnforceSorted(true);
            multi.executeWithoutAccessCheck();

            Integer pos = 0;
            if (multi.next()) {
                final Integer posTmp = multi.<Integer>getAttribute(CIProducts.TransactionAbstract.Position);
                if (posTmp != null) {
                    pos = posTmp;
                }
            }
            pos = pos + 1;
            final Update update = new Update(_instance);
            update.add(CIProducts.TransactionAbstract.Position, pos);
            update.executeWithoutTrigger();
        }
    }

    /**
     * Add or subtract from the Inventory.
     *
     * @param _parameter Parameters as passed from eFaps
     * @throws EFapsException on error
     */
    protected void addRemoveFromInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        // get the transaction
        final PrintQuery print = new PrintQuery(instance);
        final SelectBuilder selProductInst = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Product)
                        .instance();
        print.addSelect(selProductInst);
        print.addAttribute(CIProducts.TransactionAbstract.Storage,
                        CIProducts.TransactionAbstract.UoM, CIProducts.TransactionAbstract.Quantity);
        print.executeWithoutAccessCheck();

        final Instance productInst = print.getSelect(selProductInst);
        //valid product and it is not a infinite product
        if (productInst != null && productInst.isValid()
                        && !productInst.getType().isCIType(CIProducts.ProductInfinite)) {

            BigDecimal transQuantity = print.getAttribute(CIProducts.TransactionAbstract.Quantity);
            final Long storage = print.getAttribute(CIProducts.TransactionAbstract.Storage);

            final Long uomId = print.getAttribute(CIProducts.TransactionAbstract.UoM);

            final UoM uom = Dimension.getUoM(uomId);
            transQuantity = transQuantity.multiply(new BigDecimal(uom.getNumerator())).divide(
                            new BigDecimal(uom.getDenominator()), BigDecimal.ROUND_HALF_UP);

            CIType inventory;
            if (instance.getType().isKindOf(CIProducts.TransactionIndividualInbound.getType())
                             || instance.getType().isKindOf(CIProducts.TransactionIndividualOutbound.getType())) {
                inventory = CIProducts.InventoryIndividual;
            } else {
                inventory = CIProducts.Inventory;
            }

            final QueryBuilder queryBldr = new QueryBuilder(inventory);
            queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, storage);
            queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Product, productInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.InventoryAbstract.Quantity,
                               CIProducts.InventoryAbstract.Reserved);
            multi.executeWithoutAccessCheck();

            BigDecimal reserved = BigDecimal.ZERO;
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal currentReserved = BigDecimal.ZERO;
            BigDecimal currentQuantity = BigDecimal.ZERO;
            Update update;
            if (multi.next()) {
                update = new Update(multi.getCurrentInstance());
                currentReserved = multi.<BigDecimal>getAttribute(CIProducts.InventoryAbstract.Reserved);
                currentQuantity = multi.<BigDecimal>getAttribute(CIProducts.InventoryAbstract.Quantity);
            } else {
                update = new Insert(inventory);
                update.add(CIProducts.InventoryAbstract.UoM, uom.getDimension().getBaseUoM().getId());
                update.add(CIProducts.InventoryAbstract.Storage, storage);
                update.add(CIProducts.InventoryAbstract.Product, productInst);
            }
            if (instance.getType().isKindOf(CIProducts.TransactionInbound.getType())) {
                quantity = currentQuantity.add(transQuantity);
                reserved = currentReserved;
            } else if (instance.getType().isKindOf(CIProducts.TransactionOutbound.getType())) {
                quantity = currentQuantity.subtract(transQuantity);
                reserved = currentReserved;
            } else if (instance.getType().isKindOf(CIProducts.TransactionReservationInbound.getType())) {
                quantity = currentQuantity.subtract(transQuantity);
                reserved = currentReserved.add(transQuantity);
            } else if (instance.getType().isKindOf(CIProducts.TransactionReservationOutbound.getType())) {
                quantity = currentQuantity.add(transQuantity);
                reserved = currentReserved.subtract(transQuantity);
            } else if (instance.getType().isKindOf(CIProducts.TransactionIndividualInbound.getType())) {
                quantity = currentQuantity.add(transQuantity);
                reserved = currentReserved;
            } else if (instance.getType().isKindOf(CIProducts.TransactionIndividualOutbound.getType())) {
                quantity = currentQuantity.subtract(transQuantity);
                reserved = currentReserved;
            } else if (instance.getType().isKindOf(CIProducts.TransactionInbound4StaticStorage.getType())) {
                quantity = currentQuantity.add(transQuantity);
                reserved = currentReserved;
            }

            update.add(CIProducts.InventoryAbstract.Quantity, quantity);
            update.add(CIProducts.InventoryAbstract.Reserved, reserved);

            if (update.getInstance() != null && update.getInstance().isValid()
                            && quantity.compareTo(BigDecimal.ZERO) < 1 && reserved.compareTo(BigDecimal.ZERO) < 1) {
                final Delete del = new Delete(update.getInstance());
                del.executeWithoutAccessCheck();
            } else {
                update.executeWithoutAccessCheck();
            }
        }
    }

    /**
     * Gets the java script for move inventory.
     *
     * @param _parameter the _parameter
     * @return the java script4 move inventory
     * @throws EFapsException the e faps exception
     */
    public Return getJavaScript4MoveInventory(final Parameter _parameter)
        throws EFapsException
    {
        return getJavaScript4SetInventory(_parameter);
    }

    /**
     * Update fields for product for move inventory.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return updateFields4Product4MoveInventory(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Product4SetInventory(_parameter);
    }

    /**
     * Method is used as the execute event on moving products from one Storage
     * to another.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return moveInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret =  new Return();
        final Instance storageFromInst = _parameter.getInstance();
        final String date = _parameter.getParameterValue(
                        CIFormProducts.Products_InventoryMoveMassiveForm.date.name);
        final String desc = _parameter.getParameterValue(
                        CIFormProducts.Products_InventoryMoveMassiveForm.description.name);
        final String storageToId = _parameter.getParameterValue(
                        CIFormProducts.Products_InventoryMoveMassiveForm.storage.name);
        final String[] product = _parameter.getParameterValues(
                        CITableProducts.Products_InventoryMoveMassiveTable.product.name);
        final String[] quantityAr = _parameter.getParameterValues(
                        CITableProducts.Products_InventoryMoveMassiveTable.quantity.name);
        final String[] uoM = _parameter.getParameterValues(
                        CITableProducts.Products_InventoryMoveMassiveTable.uoM.name);
        final PrintQuery print = new PrintQuery(storageFromInst);
        print.addAttribute(CIProducts.StorageAbstract.Name);
        print.execute();

        final String storageFrom = print.<String>getAttribute(CIProducts.StorageAbstract.Name);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.StorageAbstract.ID, storageToId);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.StorageAbstract.Name);
        multi.execute();
        multi.next();

        final String storateTo = multi.<String>getAttribute(CIProducts.StorageAbstract.Name);

        final List<CreatedDoc> transLists = new ArrayList<CreatedDoc>();
        if (product != null && product.length > 0) {
            for (int x = 0; x < product.length; x++) {
                final Instance prodInst = Instance.get(product[x]);
                final boolean individual = prodInst.getType().isKindOf(CIProducts.ProductIndividualAbstract.getType());
                final PrintQuery prodPrint = new PrintQuery(prodInst);
                final SelectBuilder selProdInst = SelectBuilder.get()
                                .linkfrom(CIProducts.StockProductAbstract2IndividualAbstract.ToAbstract)
                                .linkto(CIProducts.StockProductAbstract2IndividualAbstract.FromAbstract).instance();
                if (individual) {
                    prodPrint.addSelect(selProdInst);
                }
                prodPrint.addAttribute(CIProducts.ProductAbstract.Description);
                prodPrint.executeWithoutAccessCheck();
                final String productDesc = prodPrint.getAttribute(CIProducts.ProductAbstract.Description);

                BigDecimal quantity = null;
                try {
                    quantity = (BigDecimal) NumberFormatter.get().getFormatter().parse(quantityAr[x]);
                } catch (final ParseException e) {
                    LOG.error("Catched ParserException", e);
                }

                final String newDesc = DBProperties.getFormatedDBProperty(Transaction.class.getName()
                        + ".moveInventory.Description", new Object[] { desc, quantityAr[x],
                            Dimension.getUoM(Long.parseLong(uoM[x])).getName(), productDesc, storageFrom, storateTo });
                Instance prodInstTmp;
                if (individual) {
                    prodInstTmp = prodPrint.getSelect(selProdInst);
                    final CreatedDoc inbound = addTransactionProduct(CIProducts.TransactionIndividualInbound,
                                    quantity, storageToId, uoM[x], date, prodInst, newDesc);

                    final CreatedDoc outbound = addTransactionProduct(CIProducts.TransactionIndividualOutbound,
                                    quantity, storageFromInst.getId(), uoM[x], date, prodInst, newDesc);
                    transLists.add(inbound);
                    transLists.add(outbound);
                } else {
                    prodInstTmp = prodInst;
                }
                final CreatedDoc inbound = addTransactionProduct(CIProducts.TransactionInbound,
                                quantity, storageToId, uoM[x], date, prodInstTmp, newDesc);

                final CreatedDoc outbound = addTransactionProduct(CIProducts.TransactionOutbound,
                                quantity, storageFromInst.getId(), uoM[x], date, prodInstTmp, newDesc);
                if (inbound.getInstance().isValid() && outbound.getInstance().isValid()) {
                    transLists.add(inbound);
                    transLists.add(outbound);
                }
            }
        }

        if (!transLists.isEmpty()) {
            final CreatedDoc doc = addTransactionDocument2ConnectTransaction(_parameter,
                            transLists.toArray(new CreatedDoc[transLists.size()]));
            Context.getThreadContext().setSessionAttribute(NAMEKEY,
                            doc.getValues().get(CIERP.DocumentAbstract.Name.name));

            final File file = new TransactionDocument().createReport(_parameter, doc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showNameFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String name = (String) Context.getThreadContext().getSessionAttribute(NAMEKEY);
        Context.getThreadContext().removeSessionAttribute(NAMEKEY);
        final StringBuilder html = new StringBuilder();
        html.append("<span style=\"text-align: center; width: 98%; font-size:40pt; height: 55px; position:absolute\">")
                        .append(name).append("</span>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * Adds the transaction product.
     *
     * @param _ciType the _ci type
     * @param _values the _values
     * @return the created doc
     * @throws EFapsException on error
     */
    protected CreatedDoc addTransactionProduct(final CIType _ciType,
                                               final Object... _values)
        throws EFapsException
    {
        final CreatedDoc createDoc = new CreatedDoc();

        final Insert insert = new Insert(_ciType);
        insert.add(CIProducts.TransactionAbstract.Quantity, _values[0]);
        createDoc.getValues().put(CIProducts.TransactionAbstract.Quantity.name, _values[0]);

        insert.add(CIProducts.TransactionInOutAbstract.Storage, _values[1]);
        createDoc.getValues().put(CIProducts.TransactionAbstract.Storage.name, _values[1]);

        insert.add(CIProducts.TransactionInOutAbstract.UoM, _values[2]);
        createDoc.getValues().put(CIProducts.TransactionAbstract.UoM.name, _values[2]);

        insert.add(CIProducts.TransactionAbstract.Date, _values[3]);
        createDoc.getValues().put(CIProducts.TransactionAbstract.Date.name, _values[3]);

        insert.add(CIProducts.TransactionAbstract.Product, _values[4]);
        createDoc.getValues().put(CIProducts.TransactionAbstract.Product.name, _values[4]);

        insert.add(CIProducts.TransactionAbstract.Description, _values[5]);
        createDoc.getValues().put(CIProducts.TransactionAbstract.Description.name, _values[5]);

        insert.execute();
        createDoc.setInstance(insert.getInstance());

        return createDoc;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _transactions transaction
     * @return createdoc for transactiondocument
     * @throws EFapsException on error
     */
    protected CreatedDoc addTransactionDocument2ConnectTransaction(final Parameter _parameter,
                                                                   final CreatedDoc... _transactions)
        throws EFapsException
    {
        CreatedDoc ret = null;
        final String productDocumentType = _parameter.getParameterValue("productDocumentType");
        if (productDocumentType != null) {
            final Instance prodDocInst = Instance.get(productDocumentType);
            if (prodDocInst.isValid()) {
                ret = new TransactionDocument().createDoc(_parameter, _transactions[0]);
                if (ret.getInstance().isValid()) {
                    // Sales_Document2ProductDocumentType
                    final Insert insert = new Insert(UUID.fromString("29438fb0-8b1f-4e4e-a409-812b2f9efdc0"));
                    insert.add("DocumentLink", ret.getInstance());
                    insert.add("DocumentTypeLink", prodDocInst);
                    insert.execute();

                    if (_transactions != null && _transactions.length > 0) {
                        for (final CreatedDoc trans : _transactions) {
                            final Update update = new Update(trans.getInstance());
                            update.add(CIProducts.TransactionAbstract.Document, ret.getInstance());
                            update.executeWithoutTrigger();
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Method is used as the validate event for moving products from one Storage
     * to another.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return validateMove(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final StringBuilder html = new StringBuilder();
        final Instance storageInst = _parameter.getInstance();
        final String[] quantities = _parameter.getParameterValues("quantity");
        final String[] products = _parameter.getParameterValues("product");

        boolean check = true;
        boolean heading = false;
        if (products != null && products.length > 0) {
            for (int y = 0; y < products.length; y++) {
                final Instance prodInst = Instance.get(products[y]);
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.InventoryAbstract);
                queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storageInst);
                queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, prodInst);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selDescr = SelectBuilder.get().linkto(CIProducts.Inventory.Product)
                                .attribute(CIProducts.ProductAbstract.Description);
                multi.addSelect(selDescr);
                multi.addAttribute(CIProducts.Inventory.Quantity);
                multi.execute();

                if (multi.next()) {
                    final BigDecimal stock = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
                    BigDecimal newStock = BigDecimal.ONE.negate();
                    try {
                        newStock = (BigDecimal) NumberFormatter.get().getFormatter().parse(quantities[y]);
                    } catch (final ParseException e) {
                        LOG.error("Catched ParserException", e);
                    }
                    if (newStock.compareTo(BigDecimal.ZERO) == -1) {
                        if (!heading) {
                            html.append(DBProperties.getProperty("esjp.Products_Transaction.validateMove.Text"))
                                .append("<br>");
                            heading = true;
                        }
                        html.append(DBProperties.getProperty("esjp.Products_Transaction.validateMove.Prod"))
                            .append(" ").append(multi.<String>getSelect(selDescr)).append(" - ")
                            .append(DBProperties.getProperty("esjp.Products_Transaction.validateMove.Stock"))
                            .append(stock).append("<br>");
                        check = false;
                    }
                }
            }
        }

        if (!check) {
            ret.put(ReturnValues.SNIPLETT, html.toString());
        } else {
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionReservactionOutbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return reservationOutboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromInventory(_parameter);
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionReservactionInbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return reservationInboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromInventory(_parameter);
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionReservactionOutbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return individualOutboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromInventory(_parameter);
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionReservactionInbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return individualInboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromInventory(_parameter);
        return new Return();
    }

    /**
     * Method for create a new transaction inbound.
     *
     * @param _parameter Parameter as passed from the efaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return executeButton(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder js = new StringBuilder();
        final Object form = null; //(UIFormCell) _parameter.get(ParameterValues.CLASS);
        if (form != null //&& form.getParent().getInstance() != null
                        ) {
            final String product = _parameter.getParameterValue("product");
            if (product != null && !product.isEmpty()) {
                final Instance instProd = Instance.get(product);
                final PrintQuery print = new PrintQuery(instProd);
                print.addAttribute(CIProducts.ProductAbstract.Description);
                print.execute();
                final Insert insert = new Insert(CIProducts.TransactionInbound);
                insert.add(CIProducts.TransactionInbound.Product, instProd.getId());
                insert.add(CIProducts.TransactionInbound.Description,
                                print.<String>getAttribute(CIProducts.ProductAbstract.Description));
                insert.add(CIProducts.TransactionInbound.UoM, _parameter.getParameterValue("uoM"));
                insert.add(CIProducts.TransactionInbound.Quantity, _parameter.getParameterValue("quantity"));
                insert.add(CIProducts.TransactionInbound.Storage, null //form.getParent().getInstance().getId()
                                );
                insert.add(CIProducts.TransactionInbound.Date, new DateTime());
                insert.execute();

                if (insert.getInstance() != null) {
                    final String description = _parameter.getParameterValue("quantity") + " - "
                                    + print.<String>getAttribute(CIProducts.ProductAbstract.Description);
                    js.append("document.getElementsByName('description')[0].innerHTML=\"")
                                    .append(description).append("\";")
                                    .append("document.getElementsByName('product')[0].value=\"\";")
                                    .append("document.getElementsByName('productAutoComplete')[0].value=\"\";")
                                    .append("document.getElementsByName('quantity')[0].value=\"\";")
                                    .append("document.getElementsByName('uoM')[0].innerHTML=\"\";");
                }
            } else {
                js.append("document.getElementsByName('description')[0].innerHTML=\"<span style='color:red;' >")
                         .append(DBProperties.getProperty("org.efaps.esjp.products.enterProduct")).append("</span>\"");
            }
        }
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    /**
     * Check the access for the document link field in the transaction formular.
     * Access will only be granted if a document is connected.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return documentAccessCheck(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final TargetMode accmod = (TargetMode) _parameter.get(ParameterValues.ACCESSMODE);
        final Instance inst = _parameter.getInstance();
        if (accmod.equals(TargetMode.VIEW) && inst.isValid()) {
            final PrintQuery print = new PrintQuery(inst);
            print.addAttribute(CIProducts.TransactionAbstract.Document);
            if (print.executeWithoutAccessCheck()) {
                final Object doc = print.getAttribute(CIProducts.TransactionAbstract.Document);
                if (doc != null) {
                    ret.put(ReturnValues.TRUE, true);
                }
            }
        }
        return ret;
    }

    /**
     * Method to change the inventory for a date.
     *
     * @param _parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return setInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String dateStr = _parameter.getParameterValue(
                        CIFormProducts.Products_InventorySet4ProductsForm.date.name);
        final DateTime date = new DateTime(dateStr);
        final String descr = _parameter
                        .getParameterValue(CIFormProducts.Products_InventorySet4ProductsForm.description.name);

        final Instance storageInst = _parameter.getCallInstance();
        final String[] products = _parameter
                        .getParameterValues(CITableProducts.Products_InventorySet4ProductsTable.product.name);
        final String[] quantities = _parameter
                        .getParameterValues(CITableProducts.Products_InventorySet4ProductsTable.quantity.name);
        final String[] uoMs = _parameter
                        .getParameterValues(CITableProducts.Products_InventorySet4ProductsTable.uoM.name);

        final List<CreatedDoc> transLists = new ArrayList<CreatedDoc>();
        if (products != null) {
            for (int i = 0; i < products.length; i++) {
                Instance productInst = Instance.get(products[i]);
                BigDecimal quantity = null;
                try {
                    quantity = (BigDecimal) NumberFormatter.get().getFormatter().parse(quantities[i]);
                } catch (final ParseException e) {
                    LOG.error("Catched ParserException", e);
                }
                final Long uoMId = Long.parseLong(uoMs[i]);

                if (productInst != null && productInst.isValid() && quantity != null && uoMId != null) {
                    final InventoryBean inventoryBean = Inventory.getInventory4Product(_parameter,
                                    storageInst, date, productInst);
                    final boolean individual = productInst.getType().isKindOf(
                                    CIProducts.ProductIndividualAbstract.getType());

                    final BigDecimal currQuantity;
                    if (inventoryBean == null) {
                        currQuantity = BigDecimal.ZERO;
                    } else {
                        currQuantity = inventoryBean.getQuantity();
                    }
                    BigDecimal moveQty;
                    CIType type;
                    if (quantity.compareTo(currQuantity) != 0) {
                        if (quantity.compareTo(currQuantity) > 0) {
                            moveQty = quantity.subtract(currQuantity);
                            type = CIProducts.TransactionInbound;
                            if (individual) {
                                final CreatedDoc trans = addTransactionProduct(CIProducts.TransactionIndividualInbound,
                                            moveQty, storageInst, uoMId, date, productInst, descr);
                                transLists.add(trans);
                                productInst = new Product().getProduct4Individual(_parameter, productInst);
                            }
                        } else {
                            moveQty = currQuantity.subtract(quantity);
                            type = CIProducts.TransactionOutbound;
                            if (individual) {
                                final CreatedDoc trans = addTransactionProduct(CIProducts.TransactionIndividualOutbound,
                                            moveQty, storageInst, uoMId, date, productInst, descr);
                                transLists.add(trans);
                                productInst = new Product().getProduct4Individual(_parameter, productInst);
                            }
                        }
                        final CreatedDoc trans = addTransactionProduct(type,
                                        moveQty, storageInst, uoMId, date, productInst, descr);
                        transLists.add(trans);
                    }
                }
            }
        }
        if (!transLists.isEmpty()) {
            final CreatedDoc doc = addTransactionDocument2ConnectTransaction(_parameter,
                            transLists.toArray(new CreatedDoc[transLists.size()]));
            Context.getThreadContext().setSessionAttribute(NAMEKEY,
                            doc.getValues().get(CIERP.DocumentAbstract.Name.name));
            final File file = new TransactionDocument().createReport(_parameter, doc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Gets the java script4 set inventory.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the java script4 set inventory
     * @throws EFapsException on error
     */
    public Return getJavaScript4SetInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Instance> instances = getSelectedInstances(_parameter);
        if (!instances.isEmpty()) {
            final StringBuilder js = new StringBuilder();
            final Set<String> noEscape = new HashSet<String>();
            noEscape.add("uoM");
            final List<Map<String, Object>> strValues = new ArrayList<>();
            final MultiPrintQuery multi = new MultiPrintQuery(instances);
            final SelectBuilder selProd = SelectBuilder.get().linkto(CIProducts.InventoryAbstract.Product);
            final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
            final SelectBuilder selProdDecr = new SelectBuilder(selProd).attribute(
                            CIProducts.ProductAbstract.Description);
            final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
            final SelectBuilder selProdInd = new SelectBuilder(selProd).attribute(
                            CIProducts.ProductAbstract.Individual);
            multi.addAttribute(CIProducts.InventoryAbstract.Quantity, CIProducts.InventoryAbstract.UoM);
            multi.addSelect(selProdInst, selProdDecr, selProdName, selProdInd);
            multi.setEnforceSorted(true);
            multi.execute();
            final DecimalFormat formater = NumberFormatter.get().getFormatter();
            while (multi.next()) {
                final Instance prodInst = multi.<Instance>getSelect(selProdInst);
                if (Products.ACTIVATEINDIVIDUAL.get() && !ProductIndividual.NONE.equals(multi.getSelect(selProdInd))) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(
                                    CIProducts.StockProductAbstract2IndividualAbstract);
                    attrQueryBldr.addWhereAttrEqValue(CIProducts.StockProductAbstract2IndividualAbstract.FromAbstract,
                                    prodInst);

                    final QueryBuilder queryBldr = new QueryBuilder(CIProducts.InventoryIndividual);
                    queryBldr.addWhereAttrEqValue(CIProducts.InventoryIndividual.Storage, _parameter.getInstance());
                    queryBldr.addWhereAttrInQuery(CIProducts.InventoryIndividual.Product, attrQueryBldr
                                    .getAttributeQuery(CIProducts.StockProductAbstract2IndividualAbstract.ToAbstract));
                    final MultiPrintQuery indMulti = queryBldr.getPrint();
                    indMulti.addSelect(selProdInst, selProdDecr, selProdName, selProdInd);
                    indMulti.addAttribute(CIProducts.InventoryIndividual.Quantity, CIProducts.InventoryAbstract.UoM);
                    indMulti.execute();
                    while (indMulti.next()) {
                        final Map<String, Object> map = new HashMap<>();
                        strValues.add(map);
                        map.put("quantity", formater.format(indMulti.getAttribute(
                                        CIProducts.InventoryAbstract.Quantity)));
                        map.put("quantityInStock", formater.format(indMulti.getAttribute(
                                        CIProducts.InventoryAbstract.Quantity)));
                        map.put("product", new String[] { indMulti.<Instance>getSelect(selProdInst).getOid(), indMulti
                                        .<String>getSelect(selProdName) });
                        map.put("productDesc", indMulti.getSelect(selProdDecr));
                        map.put("uoM", getUoMFieldStrByUoM(indMulti.<Long>getAttribute(
                                        CIProducts.InventoryAbstract.UoM)));
                    }
                } else {
                    final Map<String, Object> map = new HashMap<>();
                    strValues.add(map);
                    map.put("quantity", formater.format(multi.getAttribute(CIProducts.InventoryAbstract.Quantity)));
                    map.put("quantityInStock", formater.format(multi.getAttribute(
                                    CIProducts.InventoryAbstract.Quantity)));
                    map.put("product", new String[] { prodInst.getOid(), multi.<String>getSelect(selProdName) });
                    map.put("productDesc", multi.getSelect(selProdDecr));
                    map.put("uoM", getUoMFieldStrByUoM(multi.<Long>getAttribute(CIProducts.InventoryAbstract.UoM)));
                }
            }
            js.append(getTableRemoveScript(_parameter, "inventoryTable", false, false)).append(getTableAddNewRowsScript(
                            _parameter, "inventoryTable", strValues, null, false, false, noEscape));
            ret.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1500));
        }
        return ret;
    }

    /**
     * Update fields for quantity for set inventory.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return updateFields4Date4SetInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String[] products = _parameter.getParameterValues(
                        CITableProducts.Products_InventorySet4ProductsTable.product.name);
        final String dateStr = _parameter.getParameterValue(
                        CIFormProducts.Products_InventorySet4ProductsForm.date.name + "_eFapsDate");
        final DateTime date = DateUtil.getDateFromParameter(dateStr);

        if (!ArrayUtils.isEmpty(products)) {
            final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (int j = 0; j < products.length; j++) {
                final Map<String, Object> map = new HashMap<String, Object>();
                list.add(map);
                final Instance productInst = Instance.get(products[j]);
                if (productInst != null && productInst.isValid()) {
                    final InventoryBean inventoryBean = Inventory.getInventory4Product(_parameter,
                                _parameter.getInstance(), date, productInst);
                    final BigDecimal quantity = inventoryBean == null ? BigDecimal.ZERO : inventoryBean.getQuantity();
                    map.put(CITableProducts.Products_InventorySet4ProductsTable.quantityInStock.name,
                                NumberFormatter.get().getFormatter().format(quantity));
                }
            }
            ret.put(ReturnValues.VALUES, list);
        }
        return ret;
    }


    /**
     * Update fields for quantity for set inventory.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return updateFields4Quantity4SetInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String[] quantities = _parameter.getParameterValues(
                        CITableProducts.Products_InventorySet4ProductsTable.quantity.name);
        final String[] products = _parameter.getParameterValues(
                        CITableProducts.Products_InventorySet4ProductsTable.product.name);
        final String[] uoMs = _parameter.getParameterValues(
                        CITableProducts.Products_InventorySet4ProductsTable.uoM.name);
        final String dateStr = _parameter.getParameterValue(
                        CIFormProducts.Products_InventorySet4ProductsForm.date.name + "_eFapsDate");
        final DateTime date = DateUtil.getDateFromParameter(dateStr);

        if (!ArrayUtils.isEmpty(quantities)) {
            final int i = getSelectedRow(_parameter);
            final String quantityStr = quantities[i];
            final Instance productInst = Instance.get(products[i]);
            BigDecimal quantity = null;
            try {
                quantity = (BigDecimal) NumberFormatter.get().getFormatter().parse(quantityStr);
            } catch (final ParseException e) {
                LOG.error("Catched ParserException", e);
            }
            final Long uoMId = Long.parseLong(uoMs[i]);

            if (productInst != null && productInst.isValid() && quantity != null && uoMId != null) {
                final InventoryBean inventoryBean = Inventory.getInventory4Product(_parameter,
                                _parameter.getCallInstance(), date, productInst);
                BigDecimal currQuantity;
                if (inventoryBean == null) {
                    currQuantity = BigDecimal.ZERO;
                } else {
                    currQuantity = inventoryBean.getQuantity();
                }
                final BigDecimal moveQty = quantity.subtract(currQuantity);
                final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                final Map<String, Object> map = new HashMap<String, Object>();
                list.add(map);
                ret.put(ReturnValues.VALUES, list);
                map.put(CITableProducts.Products_InventorySet4ProductsTable.alteration.name,
                                NumberFormatter.get().getFormatter().format(moveQty));
            }
        }
        return ret;
    }

    /**
     * Update fields for product for set inventory.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return updateFields4Product4SetInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Product product = new Product() {

            @Override
            protected void add2updateFields4Product(final Parameter _parameter,
                                                    final Map<String, Object> _map)
                throws EFapsException
            {
                super.add2updateFields4Product(_parameter, _map);
                final int selected = getSelectedRow(_parameter);
                final Instance prodInst = Instance.get(_parameter.getParameterValues("product")[selected]);
                final String dateStr = _parameter.getParameterValue(
                                CIFormProducts.Products_InventorySet4ProductsForm.date.name + "_eFapsDate");
                final DateTime date = DateUtil.getDateFromParameter(dateStr);

                // validate that a product was selected
                if (prodInst.isValid()) {
                    final InventoryBean inventoryBean = Inventory.getInventory4Product(_parameter,
                                    _parameter.getCallInstance(), date, prodInst);
                    BigDecimal currQuantity;
                    if (inventoryBean == null) {
                        currQuantity = BigDecimal.ZERO;
                    } else {
                        currQuantity = inventoryBean.getQuantity();
                    }
                    _map.put(CITableProducts.Products_InventorySet4ProductsTable.quantityInStock.name,
                                    NumberFormatter.get().getFormatter().format(currQuantity));
                    _map.put(CITableProducts.Products_InventorySet4ProductsTable.quantity.name,
                                    NumberFormatter.get().getFormatter().format(currQuantity));
                }
            }
        };
        return product.updateFields4Product(_parameter);
    }

    /**
     * Validate quantity.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return validateQuantity(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<IWarning> warnings = new ArrayList<IWarning>();

        final Type transType = Type.get(Long.parseLong(_parameter
                        .getParameterValue(CIFormProducts.Products_TransactionInOutForm.type.name)));

        final Instance productInst = Instance.get(_parameter
                        .getParameterValue(CIFormProducts.Products_TransactionInOutForm.product.name));
        final BigDecimal quantity = new BigDecimal(
                        _parameter.getParameterValue(CIFormProducts.Products_TransactionAbstractForm.quantity.name));
        final String storageId = _parameter.getParameterValue(
                        CIFormProducts.Products_TransactionInOutForm.storage.name);

        BigDecimal quantityInventory = BigDecimal.ZERO;
        BigDecimal quantityReserved = BigDecimal.ZERO;

        final QueryBuilder quanInvent = new QueryBuilder(transType.isKindOf(CIProducts.TransactionIndividualAbstract)
                        ? CIProducts.InventoryIndividual : CIProducts.Inventory);
        quanInvent.addWhereAttrEqValue(CIProducts.InventoryAbstract.Product, productInst);
        quanInvent.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, storageId);

        final MultiPrintQuery multiQuanti = quanInvent.getPrint();
        multiQuanti.addAttribute(CIProducts.InventoryAbstract.Quantity);
        multiQuanti.addAttribute(CIProducts.InventoryAbstract.Reserved);
        multiQuanti.execute();

        while (multiQuanti.next()) {
            quantityInventory = multiQuanti.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            if (multiQuanti.getAttribute(CIProducts.Inventory.Reserved) != null) {
                quantityReserved = multiQuanti.<BigDecimal>getAttribute(CIProducts.Inventory.Reserved);
            }
        }

        final PrintQuery printProduct = new PrintQuery(productInst);
        printProduct.addAttribute(CIProducts.ProductAbstract.Name);
        printProduct.execute();
        final String prodName = printProduct.<String>getAttribute(CIProducts.ProductAbstract.Name);

        final PrintQuery printStorage = new PrintQuery(CIProducts.StorageAbstract.getType(), storageId);
        printStorage.addAttribute(CIProducts.StorageAbstract.Name);
        printStorage.execute();

        final String storageName = printStorage.<String>getAttribute(CIProducts.StorageAbstract.Name);

        if (transType.isCIType(CIProducts.TransactionReservationOutbound)) {
            if (quantityReserved.intValue() < quantity.intValue()) {
                warnings.add(new InsufficientStock4Transaction());
            } else {
                warnings.add(new TransactionVerify().addObject(
                                CIProducts.TransactionReservationOutbound.getType().getLabel(),
                                quantity, prodName, storageName));
            }
        } else if (transType.isCIType(CIProducts.TransactionIndividualOutbound)) {
            if (quantityInventory.intValue() < quantity.intValue()) {
                warnings.add(new InsufficientStock4Transaction());
            } else {
                warnings.add(new TransactionVerify().addObject(
                                CIProducts.TransactionIndividualOutbound.getType().getLabel(),
                                quantity, prodName, storageName));
            }
        } else if (transType.isCIType(CIProducts.TransactionInbound)) {
            warnings.add(new TransactionVerify().addObject(
                            CIProducts.TransactionInbound.getType().getLabel(),
                            quantity, prodName, storageName));
        } else if (transType.isCIType(CIProducts.TransactionIndividualInbound)) {
            warnings.add(new TransactionVerify().addObject(
                            CIProducts.TransactionIndividualInbound.getType().getLabel(),
                            quantity, prodName, storageName));
        } else if (quantityInventory.intValue() >= quantity.intValue() + quantityReserved.intValue()) {
            if (transType.isCIType(CIProducts.TransactionReservationInbound)) {
                warnings.add(new TransactionVerify().addObject(
                                CIProducts.TransactionReservationInbound.getType().getLabel(),
                                quantity, prodName, storageName));
            } else if (transType.isCIType(CIProducts.TransactionOutbound)) {
                warnings.add(new TransactionVerify().addObject(
                                CIProducts.TransactionOutbound.getType().getLabel(),
                                quantity, prodName, storageName));
            }
        } else {
            warnings.add(new InsufficientStock4Transaction());
        }
        if (warnings.isEmpty()) {
            ret.put(ReturnValues.TRUE, true);
        } else {
            ret.put(ReturnValues.SNIPLETT, WarningUtil.getHtml4Warning(warnings).toString());
            if (!WarningUtil.hasError(warnings)) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Re calculate inventory.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return reCalculateInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Instance storageInst = _parameter.getInstance();
        final SelectBuilder selProdInst = new SelectBuilder().linkto(CIProducts.TransactionAbstract.Product)
                        .instance();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInbound);
        queryBldr.addType(CIProducts.TransactionOutbound);
        queryBldr.addType(CIProducts.TransactionIndividualInbound);
        queryBldr.addType(CIProducts.TransactionIndividualOutbound);
        queryBldr.addType(CIProducts.TransactionInbound4StaticStorage);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Storage, storageInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.TransactionAbstract.Quantity,
                        CIProducts.TransactionAbstract.UoM,
                        CIProducts.TransactionAbstract.Type);
        multi.addSelect(selProdInst);
        multi.execute();

        final Map<Instance, Map<CIAttribute, Object>> map4Inventory = new HashMap<Instance, Map<CIAttribute, Object>>();
        while (multi.next()) {
            final Instance prodInst = multi.<Instance>getSelect(selProdInst);
            BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.TransactionAbstract.Quantity);
            final Long uomId = multi.<Long>getAttribute(CIProducts.TransactionAbstract.UoM);
            final Type transType = multi.<Type>getAttribute(CIProducts.TransactionAbstract.Type);
            final UoM uom = Dimension.getUoM(uomId);
            quantity = quantity.multiply(new BigDecimal(uom.getNumerator()))
                            .divide(new BigDecimal(uom.getDenominator()), BigDecimal.ROUND_HALF_UP);
            if (transType.isCIType(CIProducts.TransactionOutbound)
                            || transType.isCIType(CIProducts.TransactionIndividualOutbound)) {
                quantity = quantity.negate();
            }
            if (map4Inventory.containsKey(prodInst)) {
                final Map<CIAttribute, Object> map4Product = map4Inventory.get(prodInst);
                final BigDecimal oldQua = (BigDecimal) map4Product.get(CIProducts.TransactionAbstract.Quantity);
                map4Product.put(CIProducts.TransactionAbstract.Quantity, oldQua.add(quantity));
                map4Inventory.put(prodInst, map4Product);
            } else {
                final Map<CIAttribute, Object> map4Product = new HashMap<>();
                map4Product.put(CIProducts.TransactionAbstract.UoM, uom.getDimension().getBaseUoM().getId());
                map4Product.put(CIProducts.TransactionAbstract.Quantity, quantity);
                map4Inventory.put(prodInst, map4Product);
            }
        }

        final List<Long> validate = new ArrayList<Long>();
        if (!map4Inventory.isEmpty()) {
            for (final Entry<Instance, Map<CIAttribute, Object>> entry : map4Inventory.entrySet()) {
                final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.Inventory);
                queryBldr2.addType(CIProducts.InventoryIndividual);
                queryBldr2.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, storageInst);
                queryBldr2.addWhereAttrEqValue(CIProducts.InventoryAbstract.Product, entry.getKey());
                final MultiPrintQuery inventoryMulti = queryBldr2.getPrint();
                inventoryMulti.addAttribute(CIProducts.InventoryAbstract.Reserved);
                inventoryMulti.execute();
                Update update;
                BigDecimal value2 = BigDecimal.ZERO;
                final BigDecimal value = (BigDecimal) entry.getValue().get(CIProducts.TransactionAbstract.Quantity);
                if (inventoryMulti.next()) {
                    value2 = multi.<BigDecimal>getAttribute(CIProducts.InventoryAbstract.Reserved);
                    update = new Update(inventoryMulti.getCurrentInstance());
                } else {
                    update = new Insert(entry.getKey().getType().isKindOf(CIProducts.ProductIndividualAbstract)
                                    ? CIProducts.InventoryIndividual
                                    : CIProducts.Inventory);
                    update.add(CIProducts.InventoryAbstract.Product, entry.getKey());
                    update.add(CIProducts.InventoryAbstract.UoM,
                                    entry.getValue().get(CIProducts.TransactionAbstract.UoM));
                    update.add(CIProducts.InventoryAbstract.Storage, storageInst);
                }

                if (value2 == null) {
                    value2 = BigDecimal.ZERO;
                }

                if (value.compareTo(BigDecimal.ZERO) != 0 || value2.compareTo(BigDecimal.ZERO) != 0) {
                    update.add(CIProducts.InventoryAbstract.Quantity, value);
                    update.execute();
                    validate.add(update.getInstance().getId());
                }
            }
        }
        deleteFromInventory(storageInst, validate);
        return new Return();
    }

    /**
     * Delete from inventory.
     *
     * @param _storageInst the _storage inst
     * @param _notDelete the _not delete
     * @throws EFapsException on error
     */
    protected void deleteFromInventory(final Instance _storageInst,
                                       final List<Long> _notDelete)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        queryBldr.addType(CIProducts.InventoryIndividual);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, _storageInst);
        if (!_notDelete.isEmpty()) {
            queryBldr.addWhereAttrNotEqValue(CIProducts.Inventory.ID, _notDelete.toArray());
        }
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            final Delete delete = new Delete(query.getCurrentValue());
            delete.execute();
        }
    }

    /**
     * Sets the default storage inst.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return setDefaultStorageInst(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getCallInstance();
        if (instance != null && instance.isValid()) {
            Context.getThreadContext().setSessionAttribute(Transaction_Base.STORAGEINSTKEY, instance);
        }
        return new Return();
    }

    /**
     * The Class TransDateProd.
     */
    public static class TransDateProd
        implements Serializable
    {
        /**
         * Serializable.
         */
        private static final long serialVersionUID = 1L;

        /** The date. */
        private final DateTime date;

        /** The prod id. */
        private final Long prodId;

        /**
         * Instantiates a new trans date prod.
         *
         * @param _date the _date
         * @param _prodId the _prod id
         */
        public TransDateProd(final DateTime _date,
                             final Long _prodId)
        {
            this.date = _date;
            this.prodId = _prodId;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }

        /**
         * Getter method for the instance variable {@link #prodId}.
         *
         * @return value of instance variable {@link #prodId}
         */
        public Long getProdId()
        {
            return this.prodId;
        }
    }

    /**
     * The Class TransactionDocument.
     */
    public class TransactionDocument
        extends CommonDocument
    {

        /**
         * Instantiates a new transaction document.
         */
        public TransactionDocument()
        {
        }

        /**
         * Creates the doc.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _createDoc the _create doc
         * @return the created doc
         * @throws EFapsException on error
         */
        public CreatedDoc createDoc(final Parameter _parameter,
                                     final CreatedDoc _createDoc)
            throws EFapsException
        {
            final CreatedDoc createdDoc = new CreatedDoc();
            final Insert insert = new Insert(getType4DocCreate(_parameter));

            final String name = getDocName4Create(_parameter);
            if (name != null) {
                insert.add(CIERP.DocumentAbstract.Name, name);
                createdDoc.getValues().put(CIERP.DocumentAbstract.Name.name, name);
            }

            final Object date = _createDoc.getValues().get(CIProducts.TransactionAbstract.Date.name);
            if (date != null) {
                insert.add(CIERP.DocumentAbstract.Date, date);
                createdDoc.getValues().put(CIERP.DocumentAbstract.Date.name, date);
            }

            addStatus2DocCreate(_parameter, insert, createdDoc);
            add2DocCreate(_parameter, insert, createdDoc);
            insert.execute();

            createdDoc.setInstance(insert.getInstance());
            return createdDoc;
        }

        @Override
        public File createReport(final Parameter _parameter,
                                 final CreatedDoc _createdDoc)
            throws EFapsException
        {
            return super.createReport(_parameter, _createdDoc);
        }

        @Override
        protected Type getType4DocCreate(final Parameter _parameter)
            throws EFapsException
        {
            Type typeTransactionDoc = null;
            final Map<?, ?> properties = (HashMap<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            if (properties != null) {
                if (properties.containsKey("TransactionDocument")) {
                    typeTransactionDoc = Type.get(UUID.fromString((String) properties.get("TransactionDocument")));
                }
            }
            return typeTransactionDoc;
        }
    }

    /**
     * Warning for insufficient Stock name.
     */
    public static class InsufficientStock4Transaction
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public InsufficientStock4Transaction()
        {
            setError(true);
        }
    }

    /**
     * Warning for insufficient Stock name.
     */
    public static class TransactionVerify
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public TransactionVerify()
        {
        }
    }
}

