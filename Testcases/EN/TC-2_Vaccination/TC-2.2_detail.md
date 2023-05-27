## TC 2.2: Vaccination detail

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

The user opens the vaccination list view. The vaccination module displays the vaccinations stored in the Immunization Administration documents of the EPR.

The user selects one entry to view the details.

### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The vaccination module is properly configured to communicate with the Identity Provider and the EPR test system.
- The patient ID is known in the EPR test system.
- A set of vaccination documents for the patient is present in the EPR test system.

### Test run:

The test shall be run as follows:

Iteration:
- Start the vaccination module from the EPR portal as assistant.
- Choose vaccinations from the menu and open the view.
- Select one entry from the list via double click.
- Verify that the details of the vaccinations from the EPR test system are displayed.

Iteration:
- Start the vaccination module from the EPR portal as health professional.
- Choose vaccinations from the menu and open the view.
- Select one entry from the list via double click.
- Verify that the details of the vaccinations from the EPR test system are displayed.

Iteration:
- Start the vaccination module from the EPR portal as patient.
- Choose vaccinations from the menu and open the view.
- Select one entry from the list via double click.
- Verify that the details of the vaccinations from the EPR test system are displayed.

Iteration:
- Start the vaccination module from the EPR portal as representative.
- Choose vaccinations from the menu and open the view.
- Select one entry from the list via double click.
- Verify that the details of the vaccinations from the EPR test system are displayed.

### Expected result:

The expected result is:
- The module displays a list of vaccination ordered by date.
- The detail view opens by double clicking on an entry.
- The vaccination details are displayed with the following attributes:
1.	Date of vaccination (occurenceDateTime)
2.	Vaccine (vaccineCode)
3.	Vaccination protection (targetDesease)
4.	Dose (doseNumberPositiveInt)
5.	Lot number (lotnumber)
6.	Reason for vaccination (reasonCode)
7.	Name of the health professional who performed the vaccination (performer).
8.  Organization of the performer (organization).
