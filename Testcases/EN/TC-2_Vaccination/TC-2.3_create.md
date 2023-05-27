## TC 2.3: Create Vaccination

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

The user opens the vaccination list view. The vaccination module displays the vaccinations stored in the Immunization Administration documents of the EPR.

The user opens the form to add an entry using the (+) button.

### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The vaccination module is properly configured to communicate with the Identity Provider and the EPR test system.
- The patient ID is known in the EPR test system.

### Test run:

The test shall be run as follows:

Iteration:
- Start the vaccination module from the EPR portal as assistant.
- Choose vaccinations from the menu and open the view.
- Open the add form by clicking on the (+) button.
- Set the required attributes in the form.
- Add optional attributes in the form.
- Optionally add a comment.
- Select a confidentiality and confirm the entry to add in the conformation dialog.

Iteration:
- Start the vaccination module from the EPR portal as health professional.
- Choose vaccinations from the menu and open the view.
- Open the add form by clicking on the (+) button.
- Set the required attributes in the form.
- Add optional attributes in the form.
- Optionally add a comment.
- Select a confidentiality and confirm the entry to add in the conformation dialog.

Iteration:
- Start the vaccination module from the EPR portal as patient.
- Choose vaccinations from the menu and open the view.
- Open the add form by clicking on the (+) button.
- Set the required attributes in the form.
- Add optional attributes in the form.
- Optionally add a comment.
- Select a confidentiality and confirm the entry to add in the conformation dialog.

Iteration:
- Start the vaccination module from the EPR portal as representative.
- Choose vaccinations from the menu and open the view.
- Open the add form by clicking on the (+) button.
- Set the required attributes in the form.
- Add optional attributes in the form.
- Optionally add a comment.
- Select a confidentiality and confirm the entry to add in the conformation dialog.

### Expected result:

The expected result is:
- The module displays a list of vaccination ordered by date.
- The create form is activated by clicking the button (+).
- A new document with the edited attributes is stored in the EPR.   

The following attributes can be edited in the form:
1.	Date of vaccination
2.	Vaccine
3.	Vaccination protection
4.	Dose
5.	Lot number (optional)
6.	Reason for vaccination (optional)
7.	Name of the health professional who performed the vaccination.
8.  Organization of the performer.
