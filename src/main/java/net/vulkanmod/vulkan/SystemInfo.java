package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;
import oshi.hardware.CentralProcessor;

import java.io.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemInfo {
    public static final String cpuInfo;

    static {
        CentralProcessor centralProcessor = null;
        // Disabling OSHI is a bit useful on Asahi because it returns an invalid cpu string for some reason
        if(!Boolean.parseBoolean(System.getProperty("net.vulkanmod.skip-oshi"))) {
            // Opening F3 crashes the game on one specific platform because OSHI class fails to initialize
            // I don't want to fix OSHI for now, so let's just handle error instead of crashing
            try {
                centralProcessor = new oshi.SystemInfo().getHardware().getProcessor();
            } catch (NoClassDefFoundError e) {
                Initializer.LOGGER.warn("Failed to initialize OSHI class!");
            }
        }
        // TODO: Add more platform support?
        cpuInfo = centralProcessor != null ?
                String.format("%s", centralProcessor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ") :
                getCpuInfoLinux();
    }
    /* Returns vendor cpu string from /proc/cpuinfo. Only Linux. */
    private static String getCpuInfoLinux(){
        try {
            // TODO: support cluster systems (big.LITTLE)
            if (SystemUtils.IS_OS_LINUX) {
                BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
                String l;
                int impl = 0;
                int part = 0;
                while ((l = br.readLine()) != null) {
                    String[] sp = l.split(":");
                    // TODO: Is it really a model name?
                    if (sp[0].contains("Model name")) {
                        br.close();
                        return sp[1].trim();
                    }
                    if (sp[0].contains("CPU implementer")) {
                        impl = Integer.decode(sp[1].replace("\s", ""));
                    }
                    if (sp[0].contains("CPU part"))
                        part = Integer.decode(sp[1].replace("\s", ""));
                }
                // We're done now
                br.close();
                // TODO: Optimize a bit
                String vendor = CpuId.getImplementerName(impl);
                String model = CpuId.getPartName(impl, part);
                return vendor + " " + model + " (x" + Runtime.getRuntime().availableProcessors() + ")";
            }
        } catch (IOException e){
            Initializer.LOGGER.warn("Failed to fetch cpu string from /proc/cpuinfo. Returning stub");
        }
        return "Unknown";

    }

    // Derived from util-linux: sys-utils/lscpu-arm.c
    /*
     * SPDX-License-Identifier: GPL-2.0-or-later
     *
     * This program is free software; you can redistribute it and/or modify
     * it under the terms of the GNU General Public License as published by
     * the Free Software Foundation; either version 2 of the License, or
     * (at your option) any later version.
     *
     * Copyright (C) 2018 Riku Voipio <riku.voipio@iki.fi>
     *
     * The information here is gathered from
     *  - ARM manuals
     *  - Linux kernel: arch/armX/include/asm/cputype.h
     *  - GCC sources: config/arch/arch-cores.def
     *  - Ancient wisdom
     *  - SMBIOS tables (if applicable)
     */
    private class CpuId {

        /* CPU part */

        private static final Object[][] arm_part = {
                { 0x810, "ARM810" },
                { 0x920, "ARM920" },
                { 0x922, "ARM922" },
                { 0x926, "ARM926" },
                { 0x940, "ARM940" },
                { 0x946, "ARM946" },
                { 0x966, "ARM966" },
                { 0xa20, "ARM1020" },
                { 0xa22, "ARM1022" },
                { 0xa26, "ARM1026" },
                { 0xb02, "ARM11 MPCore" },
                { 0xb36, "ARM1136" },
                { 0xb56, "ARM1156" },
                { 0xb76, "ARM1176" },
                { 0xc05, "Cortex-A5" },
                { 0xc07, "Cortex-A7" },
                { 0xc08, "Cortex-A8" },
                { 0xc09, "Cortex-A9" },
                { 0xc0d, "Cortex-A17" },	/* Originally A12 */
                { 0xc0f, "Cortex-A15" },
                { 0xc0e, "Cortex-A17" },
                { 0xc14, "Cortex-R4" },
                { 0xc15, "Cortex-R5" },
                { 0xc17, "Cortex-R7" },
                { 0xc18, "Cortex-R8" },
                { 0xc20, "Cortex-M0" },
                { 0xc21, "Cortex-M1" },
                { 0xc23, "Cortex-M3" },
                { 0xc24, "Cortex-M4" },
                { 0xc27, "Cortex-M7" },
                { 0xc60, "Cortex-M0+" },
                { 0xd01, "Cortex-A32" },
                { 0xd02, "Cortex-A34" },
                { 0xd03, "Cortex-A53" },
                { 0xd04, "Cortex-A35" },
                { 0xd05, "Cortex-A55" },
                { 0xd06, "Cortex-A65" },
                { 0xd07, "Cortex-A57" },
                { 0xd08, "Cortex-A72" },
                { 0xd09, "Cortex-A73" },
                { 0xd0a, "Cortex-A75" },
                { 0xd0b, "Cortex-A76" },
                { 0xd0c, "Neoverse-N1" },
                { 0xd0d, "Cortex-A77" },
                { 0xd0e, "Cortex-A76AE" },
                { 0xd13, "Cortex-R52" },
                { 0xd15, "Cortex-R82" },
                { 0xd16, "Cortex-R52+" },
                { 0xd20, "Cortex-M23" },
                { 0xd21, "Cortex-M33" },
                { 0xd22, "Cortex-M55" },
                { 0xd23, "Cortex-M85" },
                { 0xd40, "Neoverse-V1" },
                { 0xd41, "Cortex-A78" },
                { 0xd42, "Cortex-A78AE" },
                { 0xd43, "Cortex-A65AE" },
                { 0xd44, "Cortex-X1" },
                { 0xd46, "Cortex-A510" },
                { 0xd47, "Cortex-A710" },
                { 0xd48, "Cortex-X2" },
                { 0xd49, "Neoverse-N2" },
                { 0xd4a, "Neoverse-E1" },
                { 0xd4b, "Cortex-A78C" },
                { 0xd4c, "Cortex-X1C" },
                { 0xd4d, "Cortex-A715" },
                { 0xd4e, "Cortex-X3" },
                { 0xd4f, "Neoverse-V2" },
                { 0xd80, "Cortex-A520" },
                { 0xd81, "Cortex-A720" },
                { 0xd82, "Cortex-X4" },
                { 0xd84, "Neoverse-V3" },
                { 0xd85, "Cortex-X925" },
                { 0xd87, "Cortex-A725" },
                { 0xd8e, "Neoverse-N3" },
                { -1, "unknown" },
        };
        private static final Object[][] brcm_part = {
            { 0x0f, "Brahma-B15" },
            { 0x100, "Brahma-B53" },
            { 0x516, "ThunderX2" },
            { -1, "unknown" },
        };

        private static final Object[][] dec_part = {
            { 0xa10, "SA110" },
            { 0xa11, "SA1100" },
            { -1, "unknown" },
        };

        private static final Object[][] cavium_part = {
            { 0x0a0, "ThunderX" },
            { 0x0a1, "ThunderX-88XX" },
            { 0x0a2, "ThunderX-81XX" },
            { 0x0a3, "ThunderX-83XX" },
            { 0x0af, "ThunderX2-99xx" },
            { 0x0b0, "OcteonTX2" },
            { 0x0b1, "OcteonTX2-98XX" },
            { 0x0b2, "OcteonTX2-96XX" },
            { 0x0b3, "OcteonTX2-95XX" },
            { 0x0b4, "OcteonTX2-95XXN" },
            { 0x0b5, "OcteonTX2-95XXMM" },
            { 0x0b6, "OcteonTX2-95XXO" },
            { 0x0b8, "ThunderX3-T110" },
            { -1, "unknown" },
        };

        private static final Object[][] apm_part = {
            { 0x000, "X-Gene" },
            { -1, "unknown" },
        };

        private static final Object[][] qcom_part = {
            { 0x001, "Oryon" },
            { 0x00f, "Scorpion" },
            { 0x02d, "Scorpion" },
            { 0x04d, "Krait" },
            { 0x06f, "Krait" },
            { 0x201, "Kryo" },
            { 0x205, "Kryo" },
            { 0x211, "Kryo" },
            { 0x800, "Falkor-V1/Kryo" },
            { 0x801, "Kryo-V2" },
            { 0x802, "Kryo-3XX-Gold" },
            { 0x803, "Kryo-3XX-Silver" },
            { 0x804, "Kryo-4XX-Gold" },
            { 0x805, "Kryo-4XX-Silver" },
            { 0xc00, "Falkor" },
            { 0xc01, "Saphira" },
            { -1, "unknown" },
        };

        private static final Object[][] samsung_part = {
            { 0x001, "exynos-m1" },
            { 0x002, "exynos-m3" },
            { 0x003, "exynos-m4" },
            { 0x004, "exynos-m5" },
            { -1, "unknown" },
        };

        private static final Object[][] nvidia_part = {
            { 0x000, "Denver" },
            { 0x003, "Denver 2" },
            { 0x004, "Carmel" },
            { -1, "unknown" },
        };

        private static final Object[][] marvell_part = {
            { 0x131, "Feroceon-88FR131" },
            { 0x581, "PJ4/PJ4b" },
            { 0x584, "PJ4B-MP" },
            { -1, "unknown" },
        };

        private static final Object[][] apple_part = {
            { 0x000, "Swift" },
            { 0x001, "Cyclone" },
            { 0x002, "Typhoon" },
            { 0x003, "Typhoon/Capri" },
            { 0x004, "Twister" },
            { 0x005, "Twister/Elba/Malta" },
            { 0x006, "Hurricane" },
            { 0x007, "Hurricane/Myst" },
            { 0x008, "Monsoon" },
            { 0x009, "Mistral" },
            { 0x00b, "Vortex" },
            { 0x00c, "Tempest" },
            { 0x00f, "Tempest-M9" },
            { 0x010, "Vortex/Aruba" },
            { 0x011, "Tempest/Aruba" },
            { 0x012, "Lightning" },
            { 0x013, "Thunder" },
            { 0x020, "Icestorm-A14" },
            { 0x021, "Firestorm-A14" },
            { 0x022, "Icestorm-M1" },
            { 0x023, "Firestorm-M1" },
            { 0x024, "Icestorm-M1-Pro" },
            { 0x025, "Firestorm-M1-Pro" },
            { 0x026, "Thunder-M10" },
            { 0x028, "Icestorm-M1-Max" },
            { 0x029, "Firestorm-M1-Max" },
            { 0x030, "Blizzard-A15" },
            { 0x031, "Avalanche-A15" },
            { 0x032, "Blizzard-M2" },
            { 0x033, "Avalanche-M2" },
            { 0x034, "Blizzard-M2-Pro" },
            { 0x035, "Avalanche-M2-Pro" },
            { 0x036, "Sawtooth-A16" },
            { 0x037, "Everest-A16" },
            { 0x038, "Blizzard-M2-Max" },
            { 0x039, "Avalanche-M2-Max" },
            { -1, "unknown" },
        };

        private static final Object[][] faraday_part = {
            { 0x526, "FA526" },
            { 0x626, "FA626" },
            { -1, "unknown" },
        };

        private static final Object[][] intel_part = {
            { 0x200, "i80200" },
            { 0x210, "PXA250A" },
            { 0x212, "PXA210A" },
            { 0x242, "i80321-400" },
            { 0x243, "i80321-600" },
            { 0x290, "PXA250B/PXA26x" },
            { 0x292, "PXA210B" },
            { 0x2c2, "i80321-400-B0" },
            { 0x2c3, "i80321-600-B0" },
            { 0x2d0, "PXA250C/PXA255/PXA26x" },
            { 0x2d2, "PXA210C" },
            { 0x411, "PXA27x" },
            { 0x41c, "IPX425-533" },
            { 0x41d, "IPX425-400" },
            { 0x41f, "IPX425-266" },
            { 0x682, "PXA32x" },
            { 0x683, "PXA930/PXA935" },
            { 0x688, "PXA30x" },
            { 0x689, "PXA31x" },
            { 0xb11, "SA1110" },
            { 0xc12, "IPX1200" },
            { -1, "unknown" },
        };

        private static final Object[][] fujitsu_part = {
            { 0x001, "A64FX" },
            { 0x003, "MONAKA" },
            { -1, "unknown" },
        };

        private static final Object[][] hisi_part = {
            { 0xd01, "TaiShan-v110" },	/* used in Kunpeng-920 SoC */
            { 0xd02, "TaiShan-v120" },	/* used in Kirin 990A and 9000S SoCs */
            { 0xd40, "Cortex-A76" },	/* HiSilicon uses this ID though advertises A76 */
            { 0xd41, "Cortex-A77" },	/* HiSilicon uses this ID though advertises A77 */
            { -1, "unknown" },
        };

        private static final Object[][] ampere_part = {
            { 0xac3, "Ampere-1" },
            { 0xac4, "Ampere-1a" },
            { -1, "unknown" },
        };

        private static final Object[][] ft_part = {
            { 0x303, "FTC310" },
            { 0x660, "FTC660" },
            { 0x661, "FTC661" },
            { 0x662, "FTC662" },
            { 0x663, "FTC663" },
            { 0x664, "FTC664" },
            { 0x862, "FTC862" },
            { -1, "unknown" },
        };

        private static final Object[][] ms_part = {
            { 0xd49, "Azure-Cobalt-100" },
            { -1, "unknown" },
        };

        private static final Object[][] unknown_part = {
            { -1, "unknown" },
        };

        /* CPU implementer (Vendor) */

         private static final Object[][] hw_implementer = {
            { 0x41, arm_part,     "ARM" },
            { 0x42, brcm_part,    "Broadcom" },
            { 0x43, cavium_part,  "Cavium" },
            { 0x44, dec_part,     "DEC" },
            { 0x46, fujitsu_part, "FUJITSU" },
            { 0x48, hisi_part,    "HiSilicon" },
            { 0x49, unknown_part, "Infineon" },
            { 0x4d, unknown_part, "Motorola/Freescale" },
            { 0x4e, nvidia_part,  "NVIDIA" },
            { 0x50, apm_part,     "APM" },
            { 0x51, qcom_part,    "Qualcomm" },
            { 0x53, samsung_part, "Samsung" },
            { 0x56, marvell_part, "Marvell" },
            { 0x61, apple_part,   "Apple" },
            { 0x66, faraday_part, "Faraday" },
            { 0x69, intel_part,   "Intel" },
            { 0x6d, ms_part,      "Microsoft" },
            { 0x70, ft_part,      "Phytium" },
            { 0xc0, ampere_part,  "Ampere" },
            { -1,   unknown_part, "unknown" },
        };

         /* Utils */

         /* return cpu implementer array */
        public static Object[] getImplementerArray(int cpu_implementer){
            for(Object[] entry : hw_implementer){
                if((int) entry[0] == cpu_implementer)
                    return entry;
            }
            return null;
        }
         /* return cpu implementer name */
         public static String getImplementerName(int cpu_implementer){
             Object[] impl = getImplementerArray(cpu_implementer);
             if(impl != null)
                 return (String) impl[2];
             return "unknown";
         }

        /* Return cpu part struct/array (0 - id, 1 - part name)*/
        public static Object[][] getPartArray(int cpu_implementer){
            for(Object[] entry : hw_implementer){
                if((int) entry[0] == cpu_implementer)
                    return (Object[][]) entry[1];
            }
            return unknown_part;
        }

         /* return part name from part struct/array */
         public static String getPartName(Object[][] part_array, int part){
             for(Object[] entry : part_array){
                 if((int) entry[0] == part)
                     return (String) entry[1];
             }
             return "unknown";
         }
         /* return part name from cpu_implementer */
         public static String getPartName(int cpu_implementer, int part){
             return getPartName(getPartArray(cpu_implementer), part);
         }
    }
}
