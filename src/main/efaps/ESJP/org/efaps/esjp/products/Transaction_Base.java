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
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SearchQuery;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.ui.wicket.models.cell.UIFormCell;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("aa16287e-6148-41d2-a40f-05a65946ecfc")
@EFapsRevision("$Rev$")
public abstract class Transaction_Base
{

    /**
     * Method to create a Transaction manually.
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
        insert.add("Quantity", _parameter.getParameterValue("quantity"));
        insert.add("UoM",  _parameter.getParameterValue("uoM"));
        insert.add("Storage", _parameter.getParameterValue("storage"));
        insert.add("Product", Instance.get(_parameter.getParameterValue("product")).getId());
        insert.add("Description", _parameter.getParameterValue("description"));
        insert.add("Date",  _parameter.getParameterValue("date"));
        insert.execute();
        return new Return();
    }

    /**
     * Method is called from within the form Products_TransactionAbstractForm to
     * retrieve the type on  Create.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return getTypeFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);

        final Type transAbstract = CIProducts.TransactionAbstract.getType();
        final Map <String, Long> values = new TreeMap<String, Long>();
        for (final Type child : transAbstract.getChildTypes()) {
            if (!child.isAbstract()) {
                values.put(DBProperties.getProperty(child.getName() + ".Label"), child.getId());
            }
        }

        final StringBuilder html = new StringBuilder();

        html.append("<select size=\"1\" name=\"").append(fieldValue.getField().getName()).append("\">");
        for (final Map.Entry<String, Long> entry : values.entrySet()) {
            html.append("<option");

            html.append(" value=\"").append(entry.getValue()).append("\">").append(entry.getKey())
                .append("</option>");
        }
        html.append("</select>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }
    /**
     * Method is called from within the form Products_TransactionAbstractForm to
     * retrieve the value for the Storage on Edit or Create.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return getStorageFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final Instance instance = (Instance) _parameter.get(ParameterValues.CALL_INSTANCE);
        final TreeMap<String, Long> map = new TreeMap<String, Long>();
        final boolean isStorage = instance == null ? false
                                                   : instance.getType().isKindOf(Type.get("Products_StorageAbstract"));
        long actual = 0;
        if (instance != null && !isStorage) {
            final PrintQuery print = new PrintQuery(instance);
            print.addAttribute("Storage");
            if (print.execute()) {
                actual =  print.<Long>getAttribute("Storage");
            }
        }
        final SearchQuery query2 = new SearchQuery();
        query2.setQueryTypes("Products_StorageAbstract");
        query2.setExpandChildTypes(true);
        query2.addSelect("ID");
        query2.addSelect("Name");
        query2.execute();
        while (query2.next()) {
            final Long storageId = (Long) query2.get("ID");
            if (!isStorage || !storageId.equals(instance.getId())) {
                map.put((String) query2.get("Name"), (Long) query2.get("ID"));
            }
        }
        query2.close();

        final StringBuilder ret = new StringBuilder();

        ret.append("<select size=\"1\" name=\"").append(fieldValue.getField().getName()).append("\">");
        for (final Map.Entry<String, Long> entry : map.entrySet()) {
            ret.append("<option");
            if (entry.getValue().equals(actual)) {
                ret.append(" selected=\"selected\" ");
            }
            ret.append(" value=\"").append(entry.getValue()).append("\">").append(entry.getKey())
                .append("</option>");
        }
        ret.append("</select>");
        retVal.put(ReturnValues.SNIPLETT, ret.toString());
        return retVal;
    }

    /**
     * Method is executed as trigger after the insert of an Products_TransactionInbound.
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return inboundTrigger(final Parameter _parameter) throws EFapsException
    {
        addRemoveFromInventory(_parameter, true, "Quantity");
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an Products_TransactionOutbound.
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return outboundTrigger(final Parameter _parameter) throws EFapsException
    {
        addRemoveFromInventory(_parameter, false, "Quantity");
        return new Return();
    }

    /**
     * Add or subtract from the Inventory.
     * @param _parameter Parameters as passed from eFaps
     * @param _add if true the quantity will be added else subtracted
     * @throws EFapsException on error
     */
    protected void addRemoveFromInventory(final Parameter _parameter, final boolean _add, final String _attribute)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        //get the transaction
        final PrintQuery query = new PrintQuery(instance);
        query.addAttribute("Storage", "Product", "UoM", "Quantity");
        BigDecimal value = null;
        Long storage = null;
        Long product = null;
        Long uomId = null;
        if (query.execute()) {
            value = (BigDecimal) query.getAttribute("Quantity");
            storage = (Long) query.getAttribute("Storage");
            product = (Long) query.getAttribute("Product");
            uomId = (Long) query.getAttribute("UoM");
        }
        final UoM uom = Dimension.getUoM(uomId);
        value = value.multiply(new BigDecimal(uom.getNumerator())).divide(new BigDecimal(uom.getDenominator()),
                                                                                BigDecimal.ROUND_HALF_UP);

        //search for the correct inventory
        final SearchQuery query2 = new SearchQuery();
        query2.setQueryTypes("Products_Inventory");
        query2.addWhereExprEqValue("Storage", storage);
        query2.addWhereExprEqValue("Product", product);
        query2.addSelect("Quantity");
        query2.addSelect("Reserved");
        query2.addSelect("OID");
        query2.execute();

        Update update;
        BigDecimal value2 = null;
        if (query2.next()) {
            update = new Update((String) query2.get("OID"));
            final BigDecimal current = (BigDecimal) query2.get(_attribute);
            if (_add) {
                value = current.add(value);
            } else {
                value = current.subtract(value);
                if ("Quantity".equals(_attribute)) {
                    value2 = (BigDecimal) query2.get("Reserved");
                } else {
                    value2 = (BigDecimal) query2.get("Quantity");
                }
                if (value2 == null) {
                    value2 = BigDecimal.ZERO;
                }
            }
        } else {
            update = new Insert("Products_Inventory");
            update.add("UoM", ((Long) uom.getDimension().getBaseUoM().getId()).toString());
            update.add("Storage", storage.toString());
            update.add("Product", product.toString());
            if (!"Quantity".equals(_attribute)) {
                update.add("Quantity", 0);
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
    public Return moveInventory(final Parameter _parameter) throws EFapsException
    {
        final Instance fromStorageInst = _parameter.getInstance();
        final Insert inbound = new Insert("Products_TransactionInbound");

        final String quantity = _parameter.getParameterValue("quantity");
        final String prodDesc = _parameter.getParameterValue("productAutoComplete");
        final String uomStr = _parameter.getParameterValue("uoM");
        final String toStorageId = _parameter.getParameterValue("storage");

        final PrintQuery print = new PrintQuery(fromStorageInst);
        print.addAttribute("Name");
        print.execute();
        final String fromStorage = print.<String>getAttribute("Name");

        final SearchQuery query = new SearchQuery();
        query.setQueryTypes("Products_StorageAbstract");
        query.setExpandChildTypes(true);
        query.addSelect("Name");
        query.addWhereExprEqValue("ID", toStorageId);
        query.execute();
        query.next();
        final String toStorage = (String) query.get("Name");

        final StringBuilder bldr = new StringBuilder();
        bldr.append(_parameter.getParameterValue("description")).append(" - ")
            .append(DBProperties.getProperty("esjp.Products_Transaction.Move.Text")).append(" ")
            .append(quantity).append(" ")
            .append(Dimension.getUoM(Long.parseLong(uomStr)).getName()).append(" ")
            .append(prodDesc).append(" : ")
            .append(fromStorage).append(" -> ").append(toStorage);

        inbound.add("Quantity", quantity);
        inbound.add("Storage", toStorageId);
        inbound.add("UoM", uomStr);
        inbound.add("Date", _parameter.getParameterValue("date"));
        inbound.add("Product", _parameter.getParameterValue("product"));
        inbound.add("Description", bldr.toString());
        inbound.execute();

        final Insert outbound = new Insert("Products_TransactionOutbound");
        outbound.add("Quantity", quantity);
        outbound.add("Storage", ((Long) fromStorageInst.getId()).toString());
        outbound.add("UoM", uomStr);
        outbound.add("Date", _parameter.getParameterValue("date"));
        outbound.add("Product", _parameter.getParameterValue("product"));
        outbound.add("Description", bldr.toString());
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
    public Return validateMove(final Parameter _parameter) throws EFapsException
    {
        final Return ret = new Return();
        final Instance fromStorageInst = _parameter.getInstance();
        final String quantityStr = _parameter.getParameterValue("quantity");
        final String productId = _parameter.getParameterValue("product");

        final SearchQuery query = new SearchQuery();
        query.setQueryTypes("Products_Inventory");
        query.addWhereExprEqValue("Storage", fromStorageInst.getId());
        query.addWhereExprEqValue("Product", productId);
        query.addSelect("Quantity");
        query.execute();
        if (query.next()) {
            final BigDecimal existing = (BigDecimal) query.get("Quantity");
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
        return new Return();
    }

    /**
     * Method for create a new transaction inbound.
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
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CIProducts.TransactionAbstract.Document);
        if (print.executeWithoutAccessCheck()) {
            final Object doc = print.getAttribute(CIProducts.TransactionAbstract.Document);
            if (doc != null) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }
}
