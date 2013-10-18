/*
 * Copyright 2003 - 2010 The eFaps Team
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.Command;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a117dd2e-05d3-4fc0-84c6-53b47cda5eeb")
@EFapsRevision("$Rev$")
public abstract class Storage_Base
{

    public Return createFromStaticStorage(final Parameter _parameter)
        throws EFapsException
    {
        final Insert insert = new Insert(CIProducts.Warehouse);
        insert.add("Name", _parameter.getParameterValue("name"));
        if (!_parameter.getParameterValue("description").isEmpty()) {
            insert.add("Description", _parameter.getParameterValue("description"));
        }
        insert.add("Status", _parameter.getParameterValue("status"));
        insert.add("Date", _parameter.getParameterValue("Date"));
        insert.execute();

        final String fromStorageId = _parameter.getParameterValue("storage");

        final Update updateStaticInventory = new Update(CIProducts.StaticInventory.getType(), fromStorageId);
        updateStaticInventory.add(CIProducts.StaticInventory.Status,
                        Status.find(CIProducts.StorageAbstractStatus.uuid, "Inactive").getId());
        updateStaticInventory.execute();

        final QueryBuilder position = new QueryBuilder(CIProducts.StaticInventoryPosition);
        position.addWhereAttrEqValue(CIProducts.StaticInventoryPosition.StaticInventory,
                        fromStorageId);
        final MultiPrintQuery multiPosition = position.getPrint();
        multiPosition.addAttribute("Quantity", "UoM", "Product");
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
            inbound.add(CIProducts.TransactionInbound4StaticStorage.Date, _parameter.getParameterValue("date"));
            inbound.add(CIProducts.TransactionInbound4StaticStorage.Product, productId);
            //inbound.add(CIProducts.TransactionInbound4StaticStorage.Description, bldr.toString());
            inbound.execute();
        }

        return new Return();
    }

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
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storageInst.getId());
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
        transQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, storageInst.getId());
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

        final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("dd-MM-yyyy");
        dateFormat.withLocale(Context.getThreadContext().getLocale());
        final Insert snapshot = new Insert(CIProducts.Snapshot);
        snapshot.add("Name", new DateTime().toString(dateFormat));
        snapshot.add("Description", DBProperties.getProperty("org.efaps.esjp.products.Storage.descriptionSnapshot")
                        .concat(new DateTime().toString(dateFormat)));
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

    public Return createStaticInventory(final Parameter _parameter)
        throws EFapsException
    {
        final Type typeInsert = Type.get(UUID.fromString("c97d3269-944f-4f77-b2fc-3d5db6ca4430"));
        final Insert insert = new Insert(typeInsert);
        insert.add("Name", _parameter.getParameterValue("name"));
        if (!_parameter.getParameterValue("description").isEmpty()) {
            insert.add("Description", _parameter.getParameterValue("description"));
        }
        insert.add("Status", _parameter.getParameterValue("status"));
        insert.add("Date", _parameter.getParameterValue("date"));
        insert.execute();

        final Type typePos = Type.get(UUID.fromString("241f2d28-3626-4ca7-8c38-18404324080e"));
        final String[] quantities = _parameter.getParameterValues("quantity");
        if (quantities != null) {
            for (int i = 0; i < quantities.length; i++) {
                final Insert posIns = new Insert(typePos);
                final String quanStr = quantities[i];
                BigDecimal quantity = BigDecimal.ZERO;
                try {
                    quantity = (BigDecimal) (quanStr.isEmpty()
                                    ? BigDecimal.ZERO : getTwoDigitsformater().parse(quanStr));
                } catch (final ParseException e) {
                    e.printStackTrace();
                }

                final Instance productdId = Instance.get(_parameter.getParameterValues("product")[i]);
                final UoM uom = Dimension.getUoM(Long.parseLong(_parameter.getParameterValues("uoM")[i]));
                posIns.add("StaticInventory", insert.getId());
                posIns.add("Product", productdId.getId());
                posIns.add("Quantity", quantity);
                posIns.add("UoM", uom.getId());
                posIns.execute();
            }
        }

        return new Return();
    }

    /**
     * Method to get a formater.
     *
     * @return a formater
     * @throws EFapsException on error
     */
    protected DecimalFormat getTwoDigitsformater()
        throws EFapsException
    {
        return getFormater(2, 2);
    }

    /**
     * @return a formater used to format bigdecimal for the user interface
     * @param _maxFrac maximum Faction, null to deactivate
     * @param _minFrac minimum Faction, null to activate
     * @throws EFapsException on error
     */
    public DecimalFormat getFormater(final Integer _minFrac,
                                     final Integer _maxFrac)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        if (_maxFrac != null) {
            formater.setMaximumFractionDigits(_maxFrac);
        }
        if (_minFrac != null) {
            formater.setMinimumFractionDigits(_minFrac);
        }
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
    }

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
                                    ? BigDecimal.ZERO : getTwoDigitsformater().parse(quanStr));
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
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String excludeTypes = (String) properties.get("ExcludeTypes");
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, Map<String, String>> orderMap = new TreeMap<String, Map<String, String>>();

        final String key = properties.containsKey("Key") ? (String) properties.get("Key") : "OID";

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageAbstract);

        if (excludeTypes != null) {
            final String[] excludes = excludeTypes.split(";");
            for (final String exclude :excludes) {
                final Type type = Type.get(exclude);
                if (type != null) {
                    queryBldr.addWhereAttrNotEqValue(CIProducts.StorageAbstract.Type, type.getId());
                }
            }
        }
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
}
