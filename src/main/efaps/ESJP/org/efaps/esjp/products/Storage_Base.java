/*
 * Copyright 2003 - 2016 The eFaps Team
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

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.datamodel.ui.UIInterface;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.Command;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CITableProducts;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.products.util.Products;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a117dd2e-05d3-4fc0-84c6-53b47cda5eeb")
@EFapsApplication("eFapsApp-Products")
public abstract class Storage_Base
    extends CommonDocument
{

    /**
     * @param _parameter parameter as passed from the eFaps API
     * @return Return with Map fro Autocomplete field
     * @throws EFapsException on error
     */
    public Return createFromStaticStorage(final Parameter _parameter)
        throws EFapsException
    {
        final Insert insert = new Insert(CIProducts.Warehouse);
        insert.add(CIProducts.Warehouse.Name,
                        _parameter.getParameterValue(CIFormProducts.Products_StorageAbstractForm.name.name));
        if (!_parameter.getParameterValue(CIFormProducts.Products_StorageAbstractForm.description.name).isEmpty()) {
            insert.add(CIProducts.Warehouse.Description,
                            _parameter.getParameterValue(CIFormProducts.Products_StorageAbstractForm.description.name));
        }
        insert.add(CIProducts.Warehouse.Status,
                        _parameter.getParameterValue(CIFormProducts.Products_StorageAbstractForm.status.name));
        insert.add(CIProducts.Warehouse.Date,
                        _parameter.getParameterValue(CIFormProducts.Products_StorageAbstractForm.date.name));
        insert.execute();

        final String fromStorageId = _parameter
                        .getParameterValue(CIFormProducts.Products_StorageAbstractForm.storage.name);

        final Update updateStaticInventory = new Update(CIProducts.StaticInventory.getType(), fromStorageId);
        updateStaticInventory.add(CIProducts.StaticInventory.Status,
                        Status.find(CIProducts.StorageAbstractStatus.Inactive));
        updateStaticInventory.execute();

        final QueryBuilder position = new QueryBuilder(CIProducts.StaticInventoryPosition);
        position.addWhereAttrEqValue(CIProducts.StaticInventoryPosition.StaticInventory,
                        fromStorageId);
        final MultiPrintQuery multiPosition = position.getPrint();
        multiPosition.addAttribute(CIProducts.StaticInventoryPosition.Quantity,
                        CIProducts.StaticInventoryPosition.UoM, CIProducts.StaticInventoryPosition.Product);
        multiPosition.execute();

        while (multiPosition.next()) {
            final BigDecimal quantity = multiPosition
                            .<BigDecimal>getAttribute(CIProducts.StaticInventoryPosition.Quantity);
            final Long uoMId = multiPosition.<Long>getAttribute(CIProducts.StaticInventoryPosition.UoM);
            final Long productId = multiPosition.<Long>getAttribute(CIProducts.StaticInventoryPosition.Product);
            final Insert inbound = new Insert(CIProducts.TransactionInbound4StaticStorage);
            inbound.add(CIProducts.TransactionInbound4StaticStorage.Quantity, quantity);
            inbound.add(CIProducts.TransactionInbound4StaticStorage.Storage, insert.getId());
            inbound.add(CIProducts.TransactionInbound4StaticStorage.UoM, uoMId);
            inbound.add(CIProducts.TransactionInbound4StaticStorage.Date,
                            _parameter.getParameterValue(CIFormProducts.Products_StorageAbstractForm.date.name));
            inbound.add(CIProducts.TransactionInbound4StaticStorage.Product, productId);
            // inbound.add(CIProducts.TransactionInbound4StaticStorage.Description,
            // bldr.toString());
            inbound.execute();
        }

        return new Return();
    }

    /**
     * @param _parameter parameter as passed from the eFaps API
     * @return Return with Map fro Autocomplete field
     * @throws EFapsException on error
     */
    public Return createSnapshot(final Parameter _parameter)
        throws EFapsException
    {
        final String storageOid = _parameter.getParameterValue("storage");
        final String dateStr = _parameter.getParameterValue("date");
        // get a new DateTime from the ISO Date String fomr the Parameters
        final DateTime date = new DateTime(dateStr);
        final Instance storageInst = Instance.get(storageOid);

        final Map<String, BigDecimal> actual = new HashMap<String, BigDecimal>();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storageInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity);
        final SelectBuilder sel = new SelectBuilder().linkto(CIProducts.Inventory.Product).oid();
        multi.addSelect(sel);
        multi.execute();
        while (multi.next()) {
            final String oid = multi.<String>getSelect(sel);
            final BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            actual.put(oid, quantity);
        }

        final QueryBuilder transQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        transQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, storageInst);
        transQueryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, date.minusMinutes(1));
        transQueryBldr.getQuery();

        final MultiPrintQuery transMulti = transQueryBldr.getPrint();
        transMulti.addAttribute(CIProducts.TransactionInOutAbstract.Quantity,
                        CIProducts.TransactionInOutAbstract.UoM);
        final SelectBuilder transSel = new SelectBuilder().linkto(CIProducts.TransactionInOutAbstract.Product).oid();
        transMulti.addSelect(transSel);
        transMulti.execute();
        while (transMulti.next()) {
            final String oid = transMulti.<String>getSelect(transSel);
            BigDecimal quantity = transMulti.<BigDecimal>getAttribute(CIProducts.TransactionInbound.Quantity);
            final Long uoMId = transMulti.<Long>getAttribute(CIProducts.TransactionInbound.UoM);
            final UoM uoM = Dimension.getUoM(uoMId);
            quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())
                            .divide(new BigDecimal(uoM.getDenominator())));
            final Instance inst = transMulti.getCurrentInstance();
            if (inst.getType().isKindOf(CIProducts.TransactionInbound.getType())) {
                quantity = quantity.negate();
            }
            if (actual.containsKey(oid)) {
                quantity = actual.get(oid).add(quantity);
            }
            actual.put(oid, quantity);
        }
        final List<Instance> instances = new ArrayList<Instance>();
        for (final Entry<String, BigDecimal> entry : actual.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
                instances.add(Instance.get(entry.getKey()));
            }
        }
        final MultiPrintQuery multiRes = new MultiPrintQuery(instances);
        multiRes.addAttribute(CIProducts.ProductAbstract.Name,
                        CIProducts.ProductAbstract.Description,
                        CIProducts.ProductAbstract.Dimension);
        multiRes.execute();

        final PrintQuery print = new PrintQuery(storageInst);
        print.addAttribute(CIProducts.StorageAbstract.Name);
        print.executeWithoutAccessCheck();

        final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("dd-MM-yyyy");
        dateFormat.withLocale(Context.getThreadContext().getLocale());
        final Insert snapshot = new Insert(CIProducts.Snapshot);
        snapshot.add("Name", new DateTime().toString(dateFormat)
                        + " " + print.getAttribute(CIProducts.StorageAbstract.Name));
        snapshot.add("Description", DBProperties.getProperty("org.efaps.esjp.products.Storage.descriptionSnapshot")
                        .concat(" ").concat(new DateTime().toString(dateFormat)));
        snapshot.add("Status", Status.find(CIProducts.StorageAbstractStatus.uuid, "Active").getId());
        snapshot.execute();
        while (multiRes.next()) {
            final Insert snapshotPosition = new Insert(CIProducts.SnapshotPosition);
            snapshotPosition.add("Quantity", actual.get(multiRes.getCurrentInstance().getOid()));
            final UoM uom = Dimension.get(multiRes.<Long>getAttribute(CIProducts.ProductAbstract.Dimension))
                            .getBaseUoM();
            snapshotPosition.add("UoM", uom.getId());
            snapshotPosition.add("Product", multiRes.getCurrentInstance().getId());
            snapshotPosition.add("Snapshot", snapshot.getId());
            snapshotPosition.execute();
        }

        return new Return();
    }

    /**
     * Create a ststic Inventroy from the UserInterface.
     *
     * @param _parameter Parameter as passed by the efasp API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return createStaticInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Insert insert = new Insert(CIProducts.StaticInventory);
        insert.add(CIProducts.StaticInventory.Name,
                        _parameter.getParameterValue(CIFormProducts.Products_StaticInventoryForm.name.name));
        if (!_parameter.getParameterValue(CIFormProducts.Products_StaticInventoryForm.description.name).isEmpty()) {
            insert.add(CIProducts.StaticInventory.Description,
                            _parameter.getParameterValue(CIFormProducts.Products_StaticInventoryForm.description.name));
        }
        insert.add(CIProducts.StaticInventory.Status,
                        _parameter.getParameterValue(CIFormProducts.Products_StaticInventoryForm.status.name));
        insert.add(CIProducts.StaticInventory.Date,
                        _parameter.getParameterValue(CIFormProducts.Products_StaticInventoryForm.date.name));
        insert.execute();

        final String[] quantities = _parameter
                        .getParameterValues(CITableProducts.Products_StaticInventoryPositionTable.quantity.name);
        if (quantities != null) {
            for (int i = 0; i < quantities.length; i++) {
                final String quanStr = quantities[i];
                BigDecimal quantity = BigDecimal.ZERO;
                try {
                    quantity = (BigDecimal) (quanStr.isEmpty()
                                    ? BigDecimal.ZERO : NumberFormatter.get().getFormatter().parse(quanStr));
                } catch (final ParseException e) {
                    e.printStackTrace();
                }
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    final Insert posIns = new Insert(CIProducts.StaticInventoryPosition);
                    final Instance productdId = Instance.get(_parameter.getParameterValues(
                                    CITableProducts.Products_StaticInventoryPositionTable.product.name)[i]);
                    final UoM uom = Dimension.getUoM(Long.parseLong(_parameter.getParameterValues(
                                    CITableProducts.Products_StaticInventoryPositionTable.uoM.name)[i]));
                    posIns.add(CIProducts.StaticInventoryPosition.StaticInventory, insert.getInstance());
                    posIns.add(CIProducts.StaticInventoryPosition.Product, productdId.getId());
                    posIns.add(CIProducts.StaticInventoryPosition.Quantity, quantity);
                    posIns.add(CIProducts.StaticInventoryPosition.UoM, uom.getId());
                    posIns.execute();
                }
            }
        }
        return new Return();
    }

    /**
     * Method is used for an autocomplete field to get the list of Storages.
     *
     * @param _parameter parameter as passed from the eFaps API
     * @return Return with Map fro Autocomplete field
     * @throws EFapsException on error
     */
    public Return editPositions4StaticInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Instance staticInventoryInst = _parameter.getInstance();
        // Clear products related to this StaticInventoryStorage
        final QueryBuilder clearPosition = new QueryBuilder(CIProducts.StaticInventoryPosition);
        clearPosition.addWhereAttrEqValue(CIProducts.StaticInventoryPosition.StaticInventory,
                        staticInventoryInst.getId());
        final MultiPrintQuery multiPosition = clearPosition.getPrint();
        multiPosition.execute();

        while (multiPosition.next()) {
            final Delete deletePosition = new Delete(multiPosition.getCurrentInstance());
            deletePosition.executeWithoutAccessCheck();
        }

        final String[] quantities = _parameter.getParameterValues("quantity");
        if (quantities != null) {
            for (int i = 0; i < quantities.length; i++) {
                final Update posInsert = new Insert(CIProducts.StaticInventoryPosition);
                final String quanStr = quantities[i];
                BigDecimal quantity = BigDecimal.ZERO;
                try {
                    quantity = (BigDecimal) (quanStr.isEmpty()
                                    ? BigDecimal.ZERO : NumberFormatter.get().getFormatter().parse(quanStr));
                } catch (final ParseException e) {
                    e.printStackTrace();
                }

                final Instance productdId = Instance.get(_parameter.getParameterValues("product")[i]);
                final UoM uom = Dimension.getUoM(Long.parseLong(_parameter.getParameterValues("uoM")[i]));
                posInsert.add("StaticInventory", staticInventoryInst.getId());
                posInsert.add("Product", productdId.getId());
                posInsert.add("Quantity", quantity);
                posInsert.add("UoM", uom.getId());
                posInsert.execute();
            }
        }
        return new Return();
    }

    /**
     * Method is used for an autocomplete field to get the list of Storages.
     *
     * @param _parameter parameter as passed from the eFaps API
     * @return Return with Map fro Autocomplete field
     * @throws EFapsException on error
     */
    public Return autoComplete4Storage(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, Map<String, String>> orderMap = new TreeMap<String, Map<String, String>>();

        final String key = containsProperty(_parameter, "Key") ? getProperty(_parameter, "Key") : "OID";

        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
        queryBldr.addWhereAttrMatchValue(CIProducts.StorageAbstract.Name, input + "*").setIgnoreCase(true);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.StorageAbstract.Name);
        multi.addAttribute(key);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CIProducts.StorageAbstract.Name);
            final Map<String, String> map = new HashMap<String, String>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), multi.getAttribute(key).toString());
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), name);
            orderMap.put(name, map);
        }
        list.addAll(orderMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method to obtain the UoM in a select.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return with the UoM.
     * @throws EFapsException on error.
     */
    public Return getUoMFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final Object value = fieldValue.getValue();
        if (value != null) {
            Long uomId = null;
            if (value instanceof String) {
                if (!((String) value).isEmpty()) {
                    uomId = Long.parseLong((String) value);
                }
            } else if (value instanceof Long) {
                uomId = (Long) value;
            }
            if (uomId != null) {
                final UoM uom = Dimension.getUoM(uomId);
                final StringBuilder html = new StringBuilder();
                html.append("<select name=\"").append(fieldValue.getField().getName()).append("\" ")
                                .append(UIInterface.EFAPSTMPTAG).append(" size=\"1\">");
                for (final UoM aUom : uom.getDimension().getUoMs()) {
                    html.append("<option value=\"").append(aUom.getId());
                    if (uom.equals(aUom)) {
                        html.append("\" selected=\"selected");
                    }
                    html.append("\">").append(aUom.getName()).append("</option>");
                }
                html.append("</select>");
                ret.put(ReturnValues.SNIPLETT, html.toString());
            }
        }
        return ret;
    }

    /**
     * Method to verify if show Static Storage depending of a command.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return ret.
     * @throws EFapsException on error.
     */
    public Return verifyIfShowStaticStorage(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        if (_parameter.get(ParameterValues.CALL_CMD) instanceof Command) {
            final Command cmd = (Command) _parameter.get(ParameterValues.CALL_CMD);
            if (cmd.getProperty("StaticStorage") != null
                            && "true".equalsIgnoreCase(cmd.getProperty("StaticStorage"))) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Method to check if the instance is of the type Static Inventory to show
     * in other case doesn't show
     *
     * @param _parameter as passed from eFaps API.
     * @return Return ret.
     * @throws EFapsException on error.
     */
    public Return checkInstanceStaticInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Type typeStaticInventory = Type.get(UUID.fromString("c97d3269-944f-4f77-b2fc-3d5db6ca4430"));
        if (_parameter.getInstance().getType().isKindOf(typeStaticInventory)) {
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * Method to check if the instance is of the type Snapshot to show in other
     * case doesn't show
     *
     * @param _parameter as passed from eFaps API.
     * @return Return ret.
     * @throws EFapsException on error.
     */
    public Return checkInstanceSnapshot(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Type typeSnapshot = Type.get(UUID.fromString("9472ec24-da43-419c-98a3-9ec6b1c32b7a"));
        if (_parameter.getInstance().getType().isKindOf(typeSnapshot)) {
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * Method to check if the instance is of the type Static_Inventory and if it
     * is so verify the status. If it is inactive not allow to edit it.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return ret.
     * @throws EFapsException on error.
     */
    public Return checkPermissionToEditStaticInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Type typeStaticInventory = Type.get(UUID.fromString("c97d3269-944f-4f77-b2fc-3d5db6ca4430"));
        if (_parameter.getInstance().getType().isKindOf(typeStaticInventory)) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StaticInventory);
            queryBldr.addWhereAttrEqValue(CIProducts.StaticInventory.ID, _parameter.getInstance().getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.StaticInventory.Status);
            multi.execute();
            if (multi.next()) {
                final Long status = multi.<Long>getAttribute(CIProducts.StaticInventory.Status);
                final Long activeId = Status.find(CIProducts.StorageAbstractStatus.uuid, "Active").getId();
                if (activeId.equals(status)) {
                    ret.put(ReturnValues.TRUE, true);
                }
            }
        } else {
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * Get the default storage.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _key optional key to be added
     * @return instance of a storage
     * @throws EFapsException on error
     */
    protected static Instance getDefaultStorage(final Parameter _parameter,
                                                final String _key)
        throws EFapsException
    {
        Instance ret = null;
        if (_key != null) {
            ret = Products.DEFAULTWAREHOUSE.get();
        }
        if (ret == null || ret != null && !ret.isValid()) {
            ret = Products.DEFAULTWAREHOUSE.get();
        }
        return ret;
    }
}
