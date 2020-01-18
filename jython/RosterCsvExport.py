# Sample script showing how to loop through the Roster entries and export
# specific information to a .csv file.
#
# Now includes a simple FileChooser, information message dialogs and
# output of specific CV values
#
# Inspired by Bob Jacobsen's 'RosterLoop' and 'CsvToTurnouts'
#
# Note: this will replace any existing file
#
# Author: Matthew Harris, copyright 2010, 2016
# Part of the JMRI distribution

import jmri
import jmri.jmrit.roster
import org.apache.commons.csv
from javax.swing import JFileChooser, JOptionPane
from jmri.jmrit.symbolicprog import CvTableModel
import java
import java.awt
import java.io

# Define some default values

# Default filename - located in the same location as the Roster itself
# The script does show a pop-up 'Save As' dialog allowing this to be
# changed when executed
outFile = jmri.jmrit.roster.Roster.getDefault().getRosterLocation()+"roster.csv"
print "Default output file:", outFile

# Determine if to output the header or not
# Set to 'True' if required; 'False' if not
outputHeader = True

# Define functions for later use

# Write the header
# Make sure that the headers match the detail!!
def writeHeader(csvFile):
    # Write the header line
    # Entries from the Basic roster entry
    csvFile.print("RosterID")
    csvFile.print("RoadName")
    csvFile.print("RoadNumber")
    csvFile.print("Manufacturer")
    csvFile.print("Owner")
    csvFile.print("Model")
    csvFile.print("Address")
    csvFile.print("Is Long?")
    csvFile.print("Speed Limit")
    csvFile.print("Comment")
    csvFile.print("Decoder Family")
    csvFile.print("Decoder Model")
    csvFile.print("Decoder Comment")

    # Function labels
    for func in range(0,28+1):
        csvFile.print("Fn%02d" % func)

    # Now add some CV values
    csvFile.print("CV19")
    csvFile.print("CV7")
    csvFile.print("CV8")

    # Lastly, add some single bits from CV29
    csvFile.print("Speed Steps (CV29.1)")
    csvFile.print("DC mode (CV29.2)")

    # Notify the writer of the end of the header record
    csvFile.println();
    print "Header written"

# Write the value of the specified CV in the specified format
# If the CV doesn't exist, a blank is written
def writeCvValue(cvValue, format):
    if cvValue != None:
        return format % cvValue.getValue()
    else:
        return ""

# Write the details of each roster entry
# Make sure that the details match the header!!
def writeDetails(csvFile):
    # Get a list of matched roster entries;
    # the list of None's means match everything
    rosterlist = jmri.jmrit.roster.Roster.getDefault().matchingList(None, None, None, None, None, None, None)

    # now loop through the matched entries, outputing things
    for entry in rosterlist.toArray() :
        # Most parameters are text-based, so can be output directly
        csvFile.print(entry.getId())
        csvFile.print(entry.getRoadName())
        csvFile.print(entry.getRoadNumber())
        csvFile.print(entry.getMfg())
        csvFile.print(entry.getOwner())
        csvFile.print(entry.getModel())
        csvFile.print(entry.getDccAddress())

        # 'isLongAddress' is a boolean function so we need
        # to deal with outputting that in this way
        if entry.longAddress:
            csvFile.print("Yes")
        else:
            csvFile.print("No")

        # Max Speed is a number - we need to convert to a string
        csvFile.print("%d%%" % entry.getMaxSpeedPCT())

        csvFile.print(entry.getComment())
        csvFile.print(entry.getDecoderFamily())
        csvFile.print(entry.getDecoderModel())
        csvFile.print(entry.getDecoderComment())

        # Now output function labels
        for func in range(0,28+1):
            csvFile.print(entry.getFunctionLabel(func))

        # Finally, we deal with reading in the CV values and
        # outputing those we're interested in

        # First we need to create and populate the CV tables - remember 
        # to call the readFile method before trying to populate the CV tables
        cvTable = CvTableModel(None, None)
        entry.readFile()
        entry.loadCvModel(None, cvTable)  # no variables, just load CVs

        # Now we can grab the CV values we're interested in
        # Bear in mind that these need to be converted from
        # integers into strings so we use the 'writeCvValue'
        # function defined earlier.
        # These examples are all in decimal - if you require
        # hex, change "%d" to "0x%x" or "0x%X"
        csvFile.print(writeCvValue(cvTable.getCvByNumber("19"), "%d"))
        csvFile.print(writeCvValue(cvTable.getCvByNumber("7"), "%d"))
        csvFile.print(writeCvValue(cvTable.getCvByNumber("8"), "%d"))

        # Lastly, we deal with examining specific bits of CV29.
        #
        # First, we read CV29 and store it temporarily as we will then use
        # bitwise comparisons later.
        #
        # For the bitwise comparisons, use the following pattern to read the
        # individual bits:
        #
        #   if (cv29Value & {value}) == {value}:
        #       csvFile.print("bit set")
        #   else:
        #       csvFile.print("bit clear")
        #
        # where {value} is one of the following:
        #
        #   Pos  Bit  Value
        #   1st    0      1
        #   2nd    1      2
        #   3rd    2      4
        #   4th    3      8
        #   5th    4     16
        #   6th    5     32
        #   7th    6     64
        #   8th    7    128

        # OK, read the value of CV29
        cv29Value = 0;
        if cvTable.getCvByNumber("29") != None:
            cv29Value = cvTable.getCvByNumber("29").getValue()
        else:
            print "Did not find a CV29 value, using zero"
        
        # Now do the bitwise comparisons.
        # First example is speedsteps, which is the second bit 
        if (cv29Value & 2) == 2:
            csvFile.print("28/128 Steps")
        else:
            csvFile.print("14 Steps")

        # Second example is DC mode, which is the third bit 
        if (cv29Value & 4) == 4:
            csvFile.print("On")
        else:
            csvFile.print("Off")

        # Notify the writer of the end of this detail record
        csvFile.println()
        csvFile.flush()
        print "Entry", entry.getId(), "written"

# Now do the actual work here

# Create output file.
# Unless modified here, this will create the output file in the same
# location as the roster itself.
# Default behaviour is for this to be in the user preferences directory
fc = JFileChooser()
fc.setSelectedFile(java.io.File(outFile))
if (java.awt.GraphicsEnvironment.isHeadless()) :
    ret = JFileChooser.APPROVE_OPTION
else :
    ret = fc.showSaveDialog(None)

if ret == JFileChooser.APPROVE_OPTION:
    # We've got a valid filename
    outFile = fc.getSelectedFile().toString()
    print "Output file:", outFile
    csvFile = org.apache.commons.csv.CSVPrinter(java.io.BufferedWriter(java.io.FileWriter(outFile)),java.nio.charset.Charset.defaultCharset(),org.apache.commons.csv.CSVFormat.DEFAULT)

    # Output the header if required
    if outputHeader==True:
        writeHeader(csvFile)

    # Output details
    writeDetails(csvFile)

    # Flush the write buffer and close the file
    csvFile.flush()
    csvFile.close()
    print "Export complete"
    if (not java.awt.GraphicsEnvironment.isHeadless()) :
        JOptionPane.showMessageDialog(None,"Roster export completed","Roster export",JOptionPane.INFORMATION_MESSAGE)
else:
    print "No export"
    if (not java.awt.GraphicsEnvironment.isHeadless()) :
        JOptionPane.showMessageDialog(None,"Roster not exported","Roster export",JOptionPane.INFORMATION_MESSAGE)
