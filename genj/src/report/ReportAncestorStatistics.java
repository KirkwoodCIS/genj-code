/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2003
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.report.Report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

/**
 * This report prints the number of ancestors found, coverage factor,
 * implex factor and coefficient of consanguinity.
 */
public class ReportAncestorStatistics extends Report {
    private static final boolean DEBUG = false;
    private static final String STACK_SEPARATOR = "\n";
    private static final String LINE_SEPARATOR =
    "--------------------------------------------------------------------------------";
    
    private double dImplexFactor;
    Vector vecGenerationInfo = new Vector();
    private HashSet setIndi = new HashSet();
    private HashSet setCommonAncestor = new HashSet();
    private TreeMap mapImplexCommonIndi = new TreeMap();
    
    private double dConsanguinityFactor;
    private TreeMap mapConsanguinityCommonIndi = new TreeMap();
    
    private class GenerationInfo {
        int iLevel;
        
        int iPossibleCount;
        int iKnownCount;
        int iDiffCount;
        
        int iPossibleCumul;
        int iKnownCumul;
        int iDiffCumul;
        
        double dCoverage;
        double dCoverageCumul;
        double dImplex;
        
        GenerationInfo(int iLevel) {
            this.iLevel = iLevel;
        }
    }
    
    private class ConsanguinityInfo {
        public Indi indi;
        public int count;
        public double dConsanguinityFactor;
        public Stack stackIndi = new Stack();
    }
    
    /**
     * @see genj.report.Report#accepts(java.lang.Object)
     */
    public String accepts(Object context) {
        // we accept GEDCOM or Individuals
        return context instanceof Indi || context instanceof Gedcom ? getName() : null;
    }
    
    /**
     * This method actually starts this report.
     * @param context   Gedcom or Indi object.
     */
    public void start(Object context) {
        Indi indi;
        
        // The script is started with a right click on an individual with the contextual submenu
        if (context instanceof Indi) {
            indi = (Indi) context;
        }
        else {
            // No one has been given, we ask the user to select someone in the tree for analysis
            Gedcom gedcom = (Gedcom) context;
            indi = (Indi) getEntityFromUser(i18n("select_individual"), gedcom, Gedcom.INDI);
        }
        
        if (indi == null)
            return;
        
        // Initialize statistics if the report is executed several times
        clearStats();
        
        // Compute the implex factor
        computeImplexFactor(indi);
        
        // Compute the consanguinity factor
        computeConsanguinityFactor(indi);
        
        // Print header
        printHeader(indi);
        
        // Print implexe statistics
        printImplexStats();
        
        // Print consanguinity statistics
        printConsanguinityStats(indi);
    }
    
    /**
     * Initialize statistics.
     */
    private void clearStats() {
        // Clear implex statistics
        dImplexFactor = 0;
        vecGenerationInfo.clear();
        setIndi.clear();
        setCommonAncestor.clear();
        mapImplexCommonIndi.clear();
        
        // Clear consanguinity statistics
        dConsanguinityFactor = 0;
        mapConsanguinityCommonIndi.clear();
    }
    
    /**
     * Print report header.
     * @param indi  Root individual.
     */
    private void printHeader(Indi indi) {
        // Print description
        println(i18n("info"));
        println();
        
        // Print root individual
        println(i18n("root_individual"));
        println(LINE_SEPARATOR);
        println(getIndiDescription(indi));
        println();
        
        // Print factors
        println(i18n("implex_factor", new Double(dImplexFactor)));
        println(i18n("consanguinity_factor", new Double(dConsanguinityFactor)));
        println();
    }
    
    /**
     * Print implexe statistics.
     */
    private void printImplexStats() {
        // Print header
        println(align(i18n("header_implex_generation"), 10, Report.ALIGN_LEFT) +
        align(i18n("header_implex_possible"), 10, Report.ALIGN_RIGHT) +
        align(i18n("header_implex_known"), 10, Report.ALIGN_RIGHT) +
        align(i18n("header_implex_known_percent"), 10, Report.ALIGN_RIGHT) +
        align(i18n("header_implex_cumul"), 10, Report.ALIGN_RIGHT) +
        align(i18n("header_implex_cumul_percent"), 10, Report.ALIGN_RIGHT) +
        align(i18n("header_implex_diff"), 10, Report.ALIGN_RIGHT) +
        align(i18n("header_implex_implex"), 10, Report.ALIGN_RIGHT));
        println(LINE_SEPARATOR);
        
        // Iteration on generations
        Iterator itr = vecGenerationInfo.iterator();
        while (itr.hasNext()) {
            GenerationInfo info = (GenerationInfo) itr.next();
            
            // Print line
            println(align(""+info.iLevel, 10, Report.ALIGN_LEFT) +
            align(""+info.iPossibleCount, 10, Report.ALIGN_RIGHT) +
            align(""+info.iKnownCount, 10, Report.ALIGN_RIGHT) +
            align(""+info.dCoverage + "%", 10, Report.ALIGN_RIGHT) +
            align(""+info.iKnownCumul, 10, Report.ALIGN_RIGHT) +
            align(""+info.dCoverageCumul + "%", 10, Report.ALIGN_RIGHT) +
            align(""+info.iDiffCount, 10, Report.ALIGN_RIGHT) +
            align(""+info.dImplex + "%", 10, Report.ALIGN_RIGHT));
        }
        println();
        
        // Print table of common individuals
        println(i18n("header_implex_common_ancestors"));
        println(LINE_SEPARATOR);
        
        // Scan common individuals
        Collection col = mapImplexCommonIndi.values();
        itr = col.iterator();
        while (itr.hasNext()) {
            Indi indi = (Indi) itr.next();
            
            // Display individual description
            println(getIndiDescription(indi));
        }
        println();
    }
    
    /**
     * Print consanguinity statistics.
     */
    private void printConsanguinityStats(Indi indi) {
        // Print list header
        println(i18n("header_consanguinity_common_ancestors"));
        println(LINE_SEPARATOR);
        
        // Scan common individuals
        Collection col = mapConsanguinityCommonIndi.values();
        Iterator itr = col.iterator();
        while (itr.hasNext()) {
            ConsanguinityInfo info = (ConsanguinityInfo) itr.next();
            
            // Print individual description
            println(align(getIndiDescription(info.indi), 60, Report.ALIGN_LEFT) +
            align(info.dConsanguinityFactor + "", 20, Report.ALIGN_RIGHT));
            
            // Print ancestor lists
            if (DEBUG) {
                StringBuffer strLine = new StringBuffer();
                Iterator itrStack = info.stackIndi.iterator();
                while (itrStack.hasNext()) {
                    String strToken = (String) itrStack.next();
                    if (strToken.equals(STACK_SEPARATOR)) {
                        println(strLine);
                        strLine.setLength(0);
                    }
                    else {
                        if (strLine.length() != 0)
                            strLine.append(" - ");
                        strLine.append(strToken);
                    }
                }
            }
        }
    }
    
    /**
     * Returns an individual description.
     */
    private String getIndiDescription(Indi indi) {
        return i18n("entity", new String[] {indi.getId(), indi.getName()});
    }
    
    /**
     * Compute the implex factor.
     * @param indi  Root individual.
     */
    private void computeImplexFactor(Indi indi) {
        // Initialize the first generation with the selected individual
        List listIndi = new ArrayList();
        listIndi.add(indi);
        
        // Compute statistics one generation after the other
        int iLevel = 1;
        while (!listIndi.isEmpty()) {
            List listParent = new ArrayList();
            computeGeneration(iLevel, listIndi, listParent);
            listIndi = listParent;
            iLevel++;
        }
        
        // Compute cumul statistics
        int iPossibleCumul = 0;
        int iKnownCumul = 0;
        int iDiffCumul = 0;
        Iterator itr = vecGenerationInfo.iterator();
        while (itr.hasNext()) {
            GenerationInfo info = (GenerationInfo) itr.next();
            
            // Compute possible
            info.iPossibleCount = (int) Math.pow(2.0f, info.iLevel - 1);
            
            // Compute cumuls
            iPossibleCumul += info.iPossibleCount;
            iKnownCumul += info.iKnownCount;
            iDiffCumul += info.iDiffCount;
            
            // Store cumuls
            info.iPossibleCumul = iPossibleCumul;
            info.iKnownCumul = iKnownCumul;
            info.iDiffCumul = iDiffCumul;
            
            // Compute coverage
            info.dCoverage = Math.round(10000 * info.iKnownCount / info.iPossibleCount) / 100d;
            info.dCoverageCumul = Math.round(10000 * info.iKnownCumul / info.iPossibleCumul) / 100d;
            
            // Compute implex
            if (iKnownCumul != 0) {
                info.dImplex = Math.round(10000 * (info.iKnownCumul - info.iDiffCumul) / info.iKnownCumul) / 100d;
                dImplexFactor = info.dImplex;
            }
        }
    }
    
    /**
     * Add an individual and all its ancestors in the common ancestor list.
     * @param indi   Common ancestor.
     */
    private void addCommonAncestor(Indi indi) {
        if (indi == null)
            return;
        
        // Add individual to the list
        String strId = indi.getId();
        setCommonAncestor.add(strId);
        
        // Add parents to the list
        Fam famc = indi.getFamc();
        if (famc != null) {
            addCommonAncestor(famc.getWife());
            addCommonAncestor(famc.getHusband());
        }
    }
    
    /**
     * Computes statistics for the specified generation.
     * @param iLevel       Current generation level.
     * @param listIndi     Individuals of a generation.
     * @param listParent   [return] Individuals of the next generation.
     */
    private void computeGeneration(int iLevel, List listIndi, List listParent) {
        // Prepare generation information
        GenerationInfo info = new GenerationInfo(iLevel);
        vecGenerationInfo.add(info);
        
        // Scan individual of the generation
        Iterator itr = listIndi.iterator();
        while (itr.hasNext()) {
            Indi indi = (Indi) itr.next();
            
            // Get ancestor ID and search it in the list
            String strId = indi.getId();
            if (setIndi.contains(strId)) {
                // Check if this indivual has already been listed
                if (!setCommonAncestor.contains(strId)) {
                    // Print individual description
                    mapImplexCommonIndi.put(strId, indi);
                    
                    // Add individual and its ancestors to the list
                    addCommonAncestor(indi);
                }
            }
            else {
                // This is a new ancestor
                setIndi.add(strId);
                info.iDiffCount++;
            }
            
            // Count this ancestor in all case
            info.iKnownCount++;
            
            // Get parents
            Fam famc = indi.getFamc();
            if (famc != null) {
                // Get mother
                Indi indiWife = famc.getWife();
                if (indiWife != null)
                    listParent.add(indiWife);
                
                // Get father
                Indi indiHusband = famc.getHusband();
                if (indiHusband != null)
                    listParent.add(indiHusband);
            }
        }
    }
    
    /**
     * Compute consanguinity factor.
     * @param indi  Root individual.
     */
    private void computeConsanguinityFactor(Indi indi) {
        // Initialize value
        dConsanguinityFactor = 0;
        
        // If no family, nothing to do
        Fam famc = indi.getFamc();
        if (famc == null)
            return;
        
        Stack vecWife = new Stack();
        Stack vecHusband = new Stack();
        checkRightTree(famc.getWife(), 0, vecWife, famc.getHusband(), 0, vecHusband);
    }
    
    /**
     * Check the ancestors of one parent to compute the consanguinity factor.
     * @param indiRight    Current individual.
     * @param iLevelRight  Current generation level.
     * @param stackRight   Current ancestor list.
     * @param indiLeft     Current individual of other the tree.
     * @param iLevelLeft   Current generation level of other the tree.
     * @param stackLeft    Current ancestor list of other the tree.
     */
    private void checkRightTree(Indi indiRight, int iLevelRight, Stack stackRight,
    Indi indiLeft, int iLevelLeft, Stack stackLeft) {
        // Exit if an individual is missing
        if (indiRight == null || indiLeft == null)
            return;
        
        // There is consanguinity only if an individual appears in father and mother tree.
        // Search if this individual appears in the other tree
        searchInLeftTree(indiRight, iLevelRight, stackRight, indiLeft, 0, stackLeft);
        
        // Add individual ID to ancestor stack
        String strIdRight = indiRight.getId();
        stackRight.push(strIdRight);
        
        // If no family, nothing to do
        Fam famc = indiRight.getFamc();
        if (famc != null) {
            // Continue to check the tree
            // Recursive call to mother and father
            checkRightTree(famc.getWife(), iLevelRight + 1, stackRight,
            indiLeft, iLevelLeft, stackLeft);
            checkRightTree(famc.getHusband(), iLevelRight + 1, stackRight,
            indiLeft, iLevelLeft, stackLeft);
        }
        
        // Remove individual ID from ancestor stack
        stackRight.pop();
    }
    
    /**
     * Search reference individual in the the ancestors of second parent.
     * @param indiRight    Reference individual.
     * @param iLevelRight  Reference generation level.
     * @param stackRight   Reference ancestor.
     * @param indiLeft     Current individual.
     * @param iLevelLeft   Current generation level.
     * @param stackLeft    Current ancestor list.
     */
    private void searchInLeftTree(Indi indiRight, int iLevelRight, Stack stackRight,
    Indi indiLeft, int iLevelLeft, Stack stackLeft) {
        // Exit if an individual is missing
        if (indiRight == null || indiLeft == null)
            return;
        
        // Do not check further if this individual is in the ancestor stack
        // on the other tree.
        String strIdLeft = indiLeft.getId();
        if (stackRight.contains(strIdLeft))
            return;
        
        // Get ID of the reference individual and check if the current individual
        // is the same.
        String strIdRight = indiRight.getId();
        if (strIdRight == strIdLeft) {
            // Check if indivividual is already in list
            ConsanguinityInfo info = (ConsanguinityInfo) mapConsanguinityCommonIndi.get(strIdRight);
            if (info == null) {
                // Create info about individual
                info = new ConsanguinityInfo();
                info.indi = indiRight;
            }
            
            // Save ancestor list in debug mode
            if (DEBUG) {
                Iterator itrStack = stackRight.iterator();
                while (itrStack.hasNext())
                    info.stackIndi.push(itrStack.next());
                info.stackIndi.push(STACK_SEPARATOR);
                
                itrStack = stackLeft.iterator();
                while (itrStack.hasNext())
                    info.stackIndi.push(itrStack.next());
                info.stackIndi.push(STACK_SEPARATOR);
            }
            
            // Add this common person in list
            mapConsanguinityCommonIndi.put(strIdRight, info);
            
            // Compute coefficient of consanguinity for this case
            double dPower = iLevelRight + iLevelLeft + 1;
            double dConsanguinityPart = Math.pow(0.5, dPower);
            dConsanguinityFactor += dConsanguinityPart;
            info.dConsanguinityFactor += dConsanguinityPart;
            info.count++;
            return;
        }
        
        // Add individual ID to ancestor stack
        stackLeft.push(strIdLeft);
        
        // If no family, nothing to do
        Fam famc = indiLeft.getFamc();
        if (famc != null) {
            // Recursive call to mother and father
            searchInLeftTree(indiRight, iLevelRight, stackRight,
            famc.getWife(), iLevelLeft + 1, stackLeft);
            searchInLeftTree(indiRight, iLevelRight, stackRight,
            famc.getHusband(), iLevelLeft + 1, stackLeft);
        }
        
        // Remove individual ID from ancestor stack
        stackLeft.pop();
    }
}
