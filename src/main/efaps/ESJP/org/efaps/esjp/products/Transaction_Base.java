/*
 * Copyright 2003 - 2009 The eFaps Team
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.ui.wicket.models.cell.UIFormCell;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Transaction_Base.java 7003 2011-08-27 02:03:45Z jan@moxter.net
 *          $
 */
@EFapsUUID("aa16287e-6148-41d2-a40f-05a65946ecfc")
@EFapsRevision("$Rev$")
public abstract class Transaction_Base
{

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
        final String typeId = _parameter.getParameterValue("type");
        final Type type = Type.get(Long.parseLong(typeId));
        final Insert insert = new Insert(type);
        insert.add(CIProducts.TransactionAbstract.Quantity, _parameter.getParameterValue("quantity"));
        insert.add(CIProducts.TransactionAbstract.UoM, _parameter.getParameterValue("uoM"));
        insert.add(CIProducts.TransactionAbstract.Storage, _parameter.getParameterValue("storage"));
        insert.add(CIProducts.TransactionAbstract.Product,
                        Instance.get(_parameter.getParameterValue("product")).getId());
        insert.add(CIProducts.TransactionAbstract.Description, _parameter.getParameterValue("description"));
        insert.add(CIProducts.TransactionAbstract.Date, _parameter.getParameterValue("date"));
        insert.execute();
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
        addRemoveFromInventory(_parameter, true, "Quantity");
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
        addRemoveFromInventory(_parameter, false, "Quantity");
        return new Return();
    }

    /**
     * Add or subtract from the Inventory.
     *
     * @param _parameter Parameters as passed from eFaps
     * @param _add if true the quantity will be added else subtracted
     * @throws EFapsException on error
     */
    protected void addRemoveFromInventory(final Parameter _parameter,
                                          final boolean _add,
                                          final String _attribute)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        // get the transaction
        final PrintQuery query = new PrintQuery(instance);
        query.addAttribute(CIProducts.Inventory.Storage, CIProducts.Inventory.Product,
                        CIProducts.Inventory.UoM, CIProducts.Inventory.Quantity);
        BigDecimal value = null;
        Long storage = null;
        Long product = null;
        Long uomId = null;
        if (query.execute()) {
            value = (BigDecimal) query.getAttribute(CIProducts.Inventory.Quantity);
            storage = (Long) query.getAttribute(CIProducts.Inventory.Storage);
            product = (Long) query.getAttribute( CIProducts.Inventory.Product);
            uomId = (Long) query.getAttribute(CIProducts.Inventory.UoM);
        }
        final UoM uom = Dimension.getUoM(uomId);
        value = value.multiply(new BigDecimal(uom.getNumerator())).divide(new BigDecimal(uom.getDenominator()),
                        BigDecimal.ROUND_HALF_UP);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storage);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, product);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity, CIProducts.Inventory.Reserved);
        multi.execute();

        Update update;
        BigDecimal value2 = null;
        if (multi.next()) {
            update = new Update(multi.getCurrentInstance());
            final BigDecimal current = multi.<BigDecimal>getAttribute(_attribute);
            if (_add) {
                value = current.add(value);
            } else {
                value = current.subtract(value);
                if ("Quantity".equals(_attribute)) {
                    value2 = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Reserved);
                } else {
                    value2 = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
                }
                if (value2 == null) {
                    value2 = BigDecimal.ZERO;
                }
            }
        } else {
            update = new Insert(CIProducts.Inventory);
            update.add(CIProducts.Inventory.UoM, uom.getDimension().getBaseUoM().getId());
            update.add(CIProducts.Inventory.Storage, storage);
            update.add(CIProducts.Inventory.Product, product);
            if (!"Quantity".equals(_attribute)) {
                update.add(CIProducts.Inventory.Quantity, 0);
            }
        }

        if (!_add && value.compareTo(BigDecimal.ZERO) < 1 && value2.compareTo(BigDecimal.ZERO) < 1) {
            final Delete del = new Delete(update.getInstance());
            del.execute();
        } else {
            update.add(_attribute, value);
            update.execute();
        }
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
        final Instance fromStorageInst = _parameter.getInstance();


        final String quantity = _parameter.getParameterValue("quantity");
        final String prodDesc = _parameter.getParameterValue("productAutoComplete");
        final String uomStr = _parameter.getParameterValue("uoM");
        final String toStorageId = _parameter.getParameterValue("storage");

        final PrintQuery print = new PrintQuery(fromStorageInst);
        print.addAttribute(CIProducts.StorageAbstract.Name);
        print.execute();
        final String fromStorage = print.<String>getAttribute(CIProducts.StorageAbstract.Name);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.StorageAbstract.ID, toStorageId);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.StorageAbstract.Name);
        multi.execute();
        multi.next();
        final String toStorage =  multi.<String>getAttribute(CIProducts.StorageAbstract.Name);

        final StringBuilder bldr = new StringBuilder();
        bldr.append(_parameter.getParameterValue("description")).append(" - ")
                .append(DBProperties.getProperty("esjp.Products_Transaction.Move.Text")).append(" ")
                .append(quantity).append(" ")
                .append(Dimension.getUoM(Long.parseLong(uomStr)).getName()).append(" ")
                .append(prodDesc).append(" : ")
                .append(fromStorage).append(" -> ").append(toStorage);

        final Insert inbound = new Insert(CIProducts.TransactionInbound);
        inbound.add(CIProducts.TransactionInbound.Quantity, quantity);
        inbound.add(CIProducts.TransactionInbound.Storage, toStorageId);
        inbound.add(CIProducts.TransactionInbound.UoM, uomStr);
        inbound.add(CIProducts.TransactionInbound.Date, _parameter.getParameterValue("date"));
        inbound.add(CIProducts.TransactionInbound.Product, _parameter.getParameterValue("product"));
        inbound.add(CIProducts.TransactionInbound.Description, bldr.toString());
        inbound.execute();

        final Insert outbound = new Insert(CIProducts.TransactionOutbound);
        outbound.add(CIProducts.TransactionOutbound.Quantity, quantity);
        outbound.add(CIProducts.TransactionOutbound.Storage, ((Long) fromStorageInst.getId()).toString());
        outbound.add(CIProducts.TransactionOutbound.UoM, uomStr);
        outbound.add(CIProducts.TransactionOutbound.Date, _parameter.getParameterValue("date"));
        outbound.add(CIProducts.TransactionOutbound.Product, _parameter.getParameterValue("product"));
        outbound.add(CIProducts.TransactionOutbound.Description, bldr.toString());
        outbound.execute();

        return new Return();
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
        final Instance fromStorageInst = _parameter.getInstance();
        final String quantityStr = _parameter.getParameterValue("quantity");
        final String productId = _parameter.getParameterValue("product");

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, fromStorageInst.getId());
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, productId);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity);
        multi.execute();
        if (multi.next()) {
            final BigDecimal existing = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            final BigDecimal check = existing.subtract(new BigDecimal(quantityStr));
            if (check.compareTo(BigDecimal.ZERO) > -1) {
                ret.put(ReturnValues.TRUE, true);
            } else {
                final StringBuilder bldr = new StringBuilder();
                bldr.append(DBProperties.getProperty("esjp.Products_Transaction.validateMove.Text"))
                                .append(" ").append(existing);
                ret.put(ReturnValues.SNIPLETT, bldr.toString());
            }
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
        addRemoveFromInventory(_parameter, false, "Reserved");
        addRemoveFromInventory(_parameter, true, "Quantity");
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
        addRemoveFromInventory(_parameter, true, "Reserved");
        addRemoveFromInventory(_parameter, false, "Quantity");
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
        final UIFormCell form = (UIFormCell) _parameter.get(ParameterValues.CLASS);
        if (form != null && form.getParent().getInstance() != null) {
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
                insert.add(CIProducts.TransactionInbound.Storage, form.getParent().getInstance().getId());
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
    public Return restoreInventory(final Parameter _parameter)
        throws EFapsException
    {
        final String dateStr = _parameter.getParameterValue("date");
        // get a new DateTime from the ISO Date String fomr the Parameters
        final DateTime date = new DateTime(dateStr);
        final BigDecimal quantityOld = new BigDecimal(_parameter.getParameterValue("quantity"));

        final Instance storageInst = _parameter.getInstance();
        final Instance productInst = Instance.get(_parameter.getParameterValue("product"));
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storageInst.getId());
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, productInst.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity);
        multi.execute();
        while (multi.next()) {
            final BigDecimal quantityCur = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            BigDecimal quantityAux = quantityCur;

            final QueryBuilder transQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            transQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, storageInst.getId());
            transQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, productInst.getId());
            transQueryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, date.minusMinutes(1));
            final MultiPrintQuery transMulti = transQueryBldr.getPrint();
            transMulti.addAttribute(CIProducts.TransactionInOutAbstract.Quantity,
                            CIProducts.TransactionInOutAbstract.UoM);
            transMulti.execute();
            while (transMulti.next()) {
                BigDecimal quantity = transMulti.<BigDecimal>getAttribute(CIProducts.TransactionInOutAbstract.Quantity);
                final Long uoMId = transMulti.<Long>getAttribute(CIProducts.TransactionInOutAbstract.UoM);
                final UoM uoM = Dimension.getUoM(uoMId);
                quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())
                                .divide(new BigDecimal(uoM.getDenominator())));
                final Instance inst = transMulti.getCurrentInstance();
                if (inst.getType().isKindOf(CIProducts.TransactionInbound.getType())) {
                    quantity = quantity.negate();
                }
                quantity = quantityAux.add(quantity);
                quantityAux = quantity;
            }

            final BigDecimal diff = quantityOld.subtract(quantityAux);
            final BigDecimal quantityNew = quantityCur.add(diff);

            final Update update = new Update(multi.getCurrentInstance());
            update.add(CIProducts.Inventory.Quantity, quantityNew);
            update.execute();
        }

        return new Return();
    }

    public Return validateQuantity(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();

        final Long idTypeTrans = Long.parseLong(_parameter
                        .getParameterValue(CIFormProducts.Products_TransactionAbstractForm.type.name));

        final Instance productInst = Instance.get(_parameter
                        .getParameterValue(CIFormProducts.Products_TransactionAbstractForm.product.name));
        final BigDecimal quantity = new BigDecimal(
                        _parameter.getParameterValue(CIFormProducts.Products_TransactionAbstractForm.quantity.name));
        final String storageId = _parameter.getParameterValue(
                        CIFormProducts.Products_TransactionAbstractForm.storage.name);

        BigDecimal quantityInventory = BigDecimal.ZERO;
        BigDecimal quantityReserved = BigDecimal.ZERO;


        final QueryBuilder quanInvent = new QueryBuilder(CIProducts.Inventory);
        quanInvent.addWhereAttrEqValue(CIProducts.Inventory.Product, productInst.getId());
        quanInvent.addWhereAttrEqValue(CIProducts.Inventory.Storage,  storageId);

        final MultiPrintQuery multiQuanti = quanInvent.getPrint();
        multiQuanti.addAttribute(CIProducts.Inventory.Quantity);
        multiQuanti.addAttribute(CIProducts.Inventory.Reserved);
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

        final String name = printProduct.<String>getAttribute(CIProducts.ProductAbstract.Name);

        final long idReserOut = CIProducts.TransactionReservationOutbound.getType().getId();
        final long idTransIn = CIProducts.TransactionInbound.getType().getId();
        final long idReserIn = CIProducts.TransactionReservationInbound.getType().getId();
        final long idTransOut = CIProducts.TransactionOutbound.getType().getId();

        if (idTypeTrans == idReserOut) {
            if (quantityReserved.intValue() < quantity.intValue()) {
                html.append("<span>")
                                .append(quantity)
                                .append("<span><br/>")
                                .append(DBProperties
                                                .getProperty("org.efaps.esjp.products.Transaction.validateReserved"));
            } else {
                bodyTransac(html, quantity, name,
                                DBProperties.getProperty("Products_TransactionReservationOutbound.Label"), ret);
            }
        } else if (idTypeTrans == idTransIn) {
            bodyTransac(html, quantity, name, DBProperties.getProperty("Products_TransactionInbound.Label"), ret);
        } else if (quantityInventory.intValue() >= quantity.intValue() + quantityReserved.intValue()) {
            if (idTypeTrans == idReserIn) {
                bodyTransac(html, quantity, name,
                                DBProperties.getProperty("Products_TransactionReservationInbound.Label"), ret);
            } else if (idTypeTrans == idTransOut) {
                bodyTransac(html, quantity, name, DBProperties.getProperty("Products_TransactionOutbound.Label"), ret);
            }
        } else {
            html.append("<span>").append(quantity).append("<span><br/>")
                            .append(DBProperties.getProperty("org.efaps.esjp.products.Transaction.validate"));
        }
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    private void bodyTransac(final StringBuilder _html,
                             final BigDecimal _quantity,
                             final String _name,
                             final String _typeTrans,
                             final Return _ret)
    {
        _html.append("<table><tr><td colspan='2'>")
            .append(_typeTrans)
            .append("</td></tr><tr>")
            .append("<td>").append(DBProperties.getProperty("Products_ProductAbstract/Name.Label"))
            .append("</td><td>").append(_name).append("</td></tr>")
            .append("<tr><td>").append(DBProperties.getProperty("Products_TransactionAbstract/Quantity.Label"))
            .append("</td><td>").append(_quantity).append("</td>")
            .append("</tr></table>");
        _ret.put(ReturnValues.TRUE, true);
    }

    public Return reCalculateInventory(final Parameter _parameter)
        throws EFapsException
    {
        final SelectBuilder selProd = new SelectBuilder().linkto(CIProducts.TransactionAbstract.Product).instance();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Storage, _parameter.getInstance().getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.TransactionAbstract.Quantity,
                        CIProducts.TransactionAbstract.UoM,
                        CIProducts.TransactionAbstract.Type);
        multi.addSelect(selProd);
        multi.execute();

        final Map<Instance, BigDecimal> map4Inventory = new HashMap<Instance, BigDecimal>();
        while (multi.next()) {
            final Instance product = multi.<Instance>getSelect(selProd);
            BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.TransactionAbstract.Quantity);
            final Long uomId = multi.<Long>getAttribute(CIProducts.TransactionAbstract.UoM);
            final Type transType = multi.<Type>getAttribute(CIProducts.TransactionAbstract.Type);
            final UoM uom = Dimension.getUoM(uomId);
            quantity = quantity.multiply(new BigDecimal(uom.getNumerator()))
                            .divide(new BigDecimal(uom.getDenominator()), BigDecimal.ROUND_HALF_UP);
            if (transType.equals(CIProducts.TransactionOutbound.getType())) {
                quantity = quantity.negate();
            }
            if (map4Inventory.containsKey(product)) {
                map4Inventory.put(product, map4Inventory.get(product).add(quantity));
            } else {
                map4Inventory.put(product, quantity);
            }
        }

        if (!map4Inventory.isEmpty()) {
            for (final Entry<Instance, BigDecimal> entry : map4Inventory.entrySet()) {
                final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.Inventory);
                queryBldr2.addWhereAttrEqValue(CIProducts.Inventory.Storage, _parameter.getInstance().getId());
                queryBldr2.addWhereAttrEqValue(CIProducts.Inventory.Product, entry.getKey().getId());
                final InstanceQuery inventoryQuery = queryBldr2.getQuery();
                inventoryQuery.execute();
                while (inventoryQuery.next()) {
                    final Update update = new Update(inventoryQuery.getCurrentValue());
                    update.add(CIProducts.Inventory.Quantity, entry.getValue());
                    update.executeWithoutTrigger();
                }
            }
        }

        return new Return();
    }
}

