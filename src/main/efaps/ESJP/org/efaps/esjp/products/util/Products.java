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

package org.efaps.esjp.products.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.IntegerSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("96b4a9bc-bfcf-41fa-9c03-4dbc6279cf63")
@EFapsApplication("eFapsApp-Products")
@EFapsSystemConfiguration("e53cd705-e463-47dc-a400-4ace4ed72071")
public final class Products
{
    /** The base. */
    public static final String BASE = "org.efaps.products.";

    /** Products-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("e53cd705-e463-47dc-a400-4ace4ed72071");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEINDIVIDUAL = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ActivateIndividual")
                    .description(" Activate the individual management menu in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEFAMILY = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Family.Activate")
                    .description(" Activate the individual management menu in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute FAMILYSUFFIXLENGTH = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Family.SuffixLength")
                    .description("Activate the family management for materials.");


    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEGENERIC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Generic.Activate")
                    .description(" Activate the generic product management in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GENERICACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Generic.ActivateFamilies")
                    .description("Activate the family management for generics.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute GENERICFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Generic.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIALISGENERIC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.IsGeneric")
                    .description("Activate the generic product management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIALACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.ActivateFamilies")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIALACTIND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.ActivateIndividual")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute MATERIALFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.FamiliesPrefix")
                    .description("Activate the family management for materials.");


    /**
     * Singelton.
     */
    private Products()
    {
    }

    /**
     * The Enum ProductIndividual.
     *
     */
    public enum ProductIndividual
        implements IEnum
    {

        /** The none. */
        NONE,

        /** The individual. */
        INDIVIDUAL,

        /** The batch. */
        BATCH;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    /**
     * @return the SystemConfigruation for Sales
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        return SystemConfiguration.get(SYSCONFUUID);
    }
}
