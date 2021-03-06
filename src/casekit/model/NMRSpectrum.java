package casekit.model;

/* NMRSpectrum.java
*
* Copyright (C) 1997-2007  Christoph Steinbeck
*
* Contact: christoph.steinbeck@uni-jena.de
*
* This software is published and distributed under MIT License
*/

/**
* A Class to model an n-dimensional NMR spectrum,
*
*/

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.List;

public class NMRSpectrum extends ArrayList{

   /**
    * An arbitrary name that can be assigned to this spectrum for identification purposes.
    */
   public String name = "";
   /**
    * An arbitrary name to identify the type of this spectrum, like COSY, NOESY, HSQC, etc. I
    * decided not to provide static Strings with given experiment type since the there are
    * numerous experiments yielding basically identical information having different names
    */
   public String specType = "";
   /**
    * The actual spectrum, i.e. a collection of nmrSignals
    */
   // protected NMRSignal[] nmrSignals;
   /**
    * This holds sorted list of Chemical Shifts of all axes. The first dimension addresses the
    * axes, the second the shift values in this axis, starting from the highest value.
    */
   public List<List> shiftList;
   /**
    * Not yet clear if this is needed.
    */
   public float[] pickPrecision;
   /**
    * Declares how many axes are in involved in this spectrum.
    */
   public int dim = 1;
   /**
    * The nuclei of the different axes.
    */
   public String nucleus[];
   /**
    * The proton frequency of the spectrometer used to record this spectrum.
    */
   public float spectrometerFrequency;
   public String solvent = "";
   public String standard = "";
   /**
    * Some standard nulcei for the 'nucleus' field.
    */
   public static String NUC_PROTON = "1H";
   public static String NUC_CARBON = "13C";
   public static String NUC_NITROGEN = "15N";
   public static String NUC_PHOSPHORUS = "31P";
   // ... to be continued...
   public static String[] SPECTYPE_BB = {NUC_CARBON};
   public static String[] SPECTYPE_DEPT = {NUC_CARBON};
   public static String[] SPECTYPE_HMQC = {NUC_PROTON, NUC_CARBON};
   public static String[] SPECTYPE_HSQC = {NUC_PROTON, NUC_CARBON};
   public static String[] SPECTYPE_NHCORR = {NUC_PROTON, NUC_NITROGEN};
   public static String[] SPECTYPE_HMBC = {NUC_PROTON, NUC_CARBON};
   public static String[] SPECTYPE_HHCOSY = {NUC_PROTON, NUC_PROTON};
   public static String[] SPECTYPE_NOESY = {NUC_PROTON, NUC_PROTON};
   protected transient EventListenerList changeListeners = new EventListenerList();

   public NMRSpectrum(String[] nucleus, String name) {
       this.dim = nucleus.length; // redundant, I know :-)
       this.nucleus = nucleus;
       shiftList = new ArrayList<List>(dim);
       for (int f = 0; f < dim; f++) {
           shiftList.add(f, new ArrayList());
       }
       this.name = name;
   }

   /**
    * Return the number of individual frequencies in the heteroatom shift list, which should be
    * equal or smaller than the number of respective atoms
    */
   public int getSignalNumber(int axis) {
       return shiftList.get(axis).size();
   }

   /**
    * Adds an NMRSignal to the NMRSpectrum.
    */
   public void addSignal(NMRSignal thisSignal) {
       add(thisSignal);
       updateShiftLists();
   }

   /**
    * Creates an empty signal with correct dimension
    */
   public void newSignal() {
       System.out.println("nucleus: " + nucleus.length + nucleus[0]);
       add(new NMRSignal(nucleus));
       updateShiftLists();
   }

   /**
    * Returns an NMRSignal at position number in the List
    */
   public Object getSignal(int number) {
       return get(number);
   }

   /**
    * Returns the position of an NMRSignal the List
    */
   public int getSignalNumber(NMRSignal signal) {
       for (int f = 0; f < size(); f++) {
           if (((NMRSignal) get(f)) == signal) {
               return f;
           }
       }
       return -1;
   }

   public void setSpectrometerFrequency(float sf) {
       this.spectrometerFrequency = sf;
   }

   public float getSpectrometerFrequency() {
       return spectrometerFrequency;
   }

   public void setSolvent(String solvent) {
       this.solvent = solvent;
   }

   public String getSolvent() {
       return solvent;
   }

   public void setStandard(String standard) {
       this.standard = standard;
   }

   public String getStandard() {
       return standard;
   }

   /**
    * Returns the signal closest to the shift sought. If no Signal is found within the interval
    * defined by pickprecision, null is returned.
    */
   public Object pickClosestSignal(float shift, String nnucleus,
                                   float pickprecision) {
       int dim = -1, thisPosition = -1;
       float diff = Float.MAX_VALUE;
       for (int f = 0; f < nucleus.length; f++) {
           if (nucleus[f].equals(nnucleus)) {
               dim = f;
               break;
           }
       }

       /*
       * Now we search dimension dim for the chemical shift.
       */
       for (int f = 0; f < size(); f++) {
           if (diff > Math.abs(((NMRSignal) get(f)).shift[dim] - shift)) {
               diff = Math.abs(((NMRSignal) get(f)).shift[dim] - shift);
               diff = (float) Math.ceil(diff * 2) / 2;
               thisPosition = f;
           }
       }
       if (diff < pickprecision) {
           return get(thisPosition);
       }
       return null;
   }

   /**
    * Returns a List with signals within the interval defined by pickprecision. If none is found
    * an empty List is returned.
    */
   public List pickSignals(float shift, String nnucleus, float pickprecision) {
       int dim = -1;
       List pickedSignals = new ArrayList();
       for (int f = 0; f < nucleus.length; f++) {
           if (nucleus[f].equals(nnucleus)) {
               dim = f;
               break;
           }
       }
       /*
       * Now we search dimension dim for the chemical shift.
       */
       for (int f = 0; f < size(); f++) {
           if (pickprecision > Math.abs(((NMRSignal) get(f)).shift[dim]
                   - shift)) {
               pickedSignals.add(get(f));
           }
       }
       return pickedSignals;
   }

   /**
    * Extracts a list of unique shifts from the list of cross signals and sorts them. This is to
    * define the column and row headers for tables.
    */
   protected void updateShiftLists() {
       Float shift;
       for (int i = 0; i < size(); i++) {
           NMRSignal nmrSignal = (NMRSignal) get(i);
           for (int j = 0; j < nmrSignal.shift.length; j++) {
               shift = new Float(nmrSignal.shift[j]);
               if (!shiftList.get(j).contains(shift)) {
                   shiftList.get(j).add(shift);
               }
           }
       }
   }

   /**
    * Creates a 2D matrix of booleans, that models the set of crosspeaks in the 2D NMR spectrum.
    * The dimensions are taken from hetAtomShiftList and protonShiftList, which again are
    * produced by updateShiftLists based a collection of 2D nmrSignals
    * <p/>
    * private void createMatrix(){ boolean found; float het, prot; int hetPos, protPos;
    * hetCorMatrix = new boolean[hetAtomShiftList.length][protonShiftList.length]; for (int f =
    * 0; f < size(); f++){ HetCorNMRSignal hetCorSignal = (HetCorNMRSignal)elementAt(f); prot =
    * hetCorSignal.shift[NMRSignal.SHIFT_PROTON]; het =
    * hetCorSignal.shift[NMRSignal.SHIFT_HETERO]; found = false; hetPos =
    * isInShiftList(hetAtomShiftList, het, hetAtomShiftList.length); if (hetPos >= 0){ protPos =
    * isInShiftList(protonShiftList, prot, protonShiftList.length); if ( protPos >= 0){ found =
    * true; hetCorMatrix[hetPos][protPos] = true; } } } }
    */
   public void report() {
       String s = "";
       System.out.println("Report for nmr spectrum " + name + " of type "
               + specType + ": ");
       for (int i = 0; i < shiftList.size(); i++) {
           System.out.println("ShiftList for dimension " + (i + 1) + ":");
           for (int j = 0; j < shiftList.get(i).size(); j++) {
               s += shiftList.get(i).get(j) + "; ";
           }
           System.out.println(s + "\n");
           s = "";
       }

   }

}
