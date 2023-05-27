## TC 2.1: Vaccinations

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

The user opens the vaccination list view. The module displays the vaccinations stored in the Immunization Administration documents of the EPR.

### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The vaccination module is properly configured to communicate with the Identity Provider and the EPR test system.
- The patient ID is known in the EPR test system.
- A set of vaccination documents for the patient is present in the EPR test system.

### Test run:

The test shall be run as follows:

Iteration:
- Start the vaccination module from the EPR portal as assistant.
- Choose vaccinations from the menu and open the view.
- Verify that the vaccinations from the EPR test system are displayed im the list view.

Iteration:
- Start the vaccination module from the EPR portal as health professional.
- Choose vaccinations from the menu and open the view.
- Verify that the vaccinations from the EPR test system are displayed im the list view.

Iteration:
- Start the vaccination module from the EPR portal as patient.
- Choose vaccinations from the menu and open the view.
- Verify that the vaccinations from the EPR test system are displayed im the list view.

Iteration:
- Start the vaccination module from the EPR portal as representative.
- Choose vaccinations from the menu and open the view.
- Verify that the vaccinations from the EPR test system are displayed im the list view.

### Expected result:

The expected result is:
- The module displays a list of vaccination ordered by date.
- The vaccination is displayed with the following attributes:
1. Date of vaccination (occurenceDateTime)
2. Vaccination protection (targetDesease)
3. Vaccine (vaccineCode)
4. Dose (doseNumberPositiveInt)
5. Name of the health professional who performed the vaccination (performer).
