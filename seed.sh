#!/bin/bash
set -e

if [ ! -f .env ]; then
  echo "Error: .env file not found. Run this script from the project root."
  exit 1
fi

if ! command -v jq &> /dev/null; then
  echo "Error: jq is required. Install it with: sudo apt install jq"
  exit 1
fi

USERNAME=$(grep '^KEYCLOACK_USER_USERNAME=' .env | cut -d'=' -f2-)
PASSWORD=$(grep '^KEYCLOACK_USER_PASSWORD=' .env | cut -d'=' -f2-)
BASE_URL="https://api.patientsystem.me"

# ---- TOKEN ----
echo "Authenticating with Keycloak..."
TOKEN=$(curl -s -X POST "${BASE_URL}/realms/patientmanagement/protocol/openid-connect/token" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=patient-frontend" \
  --data-urlencode "username=${USERNAME}" \
  --data-urlencode "password=${PASSWORD}" | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "Error: Failed to obtain token. Check credentials in .env."
  exit 1
fi
echo "Token obtained."

post() {
  local path="$1"
  local body="$2"
  curl -s -X POST "${BASE_URL}${path}" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKEN}" \
    -d "${body}"
}

# ---- CATEGORIES ----
echo ""
echo "Creating categories..."

PHYSIO_ID=$(post "/api/categories" '{"name":"Physiotherapy","description":"Physical therapy and rehabilitation treatments"}' | jq -r '.id')
echo "  Physiotherapy → ${PHYSIO_ID}"

CARDIO_ID=$(post "/api/categories" '{"name":"Cardiology","description":"Heart and cardiovascular system treatments"}' | jq -r '.id')
echo "  Cardiology → ${CARDIO_ID}"

DERM_ID=$(post "/api/categories" '{"name":"Dermatology","description":"Skin, hair and nail treatments"}' | jq -r '.id')
echo "  Dermatology → ${DERM_ID}"

NEURO_ID=$(post "/api/categories" '{"name":"Neurology","description":"Brain and nervous system assessments"}' | jq -r '.id')
echo "  Neurology → ${NEURO_ID}"

ORTHO_ID=$(post "/api/categories" '{"name":"Orthopedics","description":"Bone, joint and muscle treatments"}' | jq -r '.id')
echo "  Orthopedics → ${ORTHO_ID}"

OPHTHO_ID=$(post "/api/categories" '{"name":"Ophthalmology","description":"Eye and vision care treatments"}' | jq -r '.id')
echo "  Ophthalmology → ${OPHTHO_ID}"

PSYCH_ID=$(post "/api/categories" '{"name":"Psychiatry","description":"Mental health assessments and treatments"}' | jq -r '.id')
echo "  Psychiatry → ${PSYCH_ID}"

GASTRO_ID=$(post "/api/categories" '{"name":"Gastroenterology","description":"Digestive system treatments"}' | jq -r '.id')
echo "  Gastroenterology → ${GASTRO_ID}"

ENDO_ID=$(post "/api/categories" '{"name":"Endocrinology","description":"Hormonal and metabolic disorder treatments"}' | jq -r '.id')
echo "  Endocrinology → ${ENDO_ID}"

PULMO_ID=$(post "/api/categories" '{"name":"Pulmonology","description":"Lung and respiratory system treatments"}' | jq -r '.id')
echo "  Pulmonology → ${PULMO_ID}"

# ---- TREATMENTS ----
echo ""
echo "Creating treatments..."

post "/api/treatments" "{\"name\":\"Physical Therapy Session\",\"category\":\"${PHYSIO_ID}\",\"price\":80.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Manual Therapy\",\"category\":\"${PHYSIO_ID}\",\"price\":95.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Exercise Rehabilitation\",\"category\":\"${PHYSIO_ID}\",\"price\":70.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Hydrotherapy\",\"category\":\"${PHYSIO_ID}\",\"price\":85.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Sports Injury Rehabilitation\",\"category\":\"${PHYSIO_ID}\",\"price\":110.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Ultrasound Therapy\",\"category\":\"${PHYSIO_ID}\",\"price\":65.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"ECG Test\",\"category\":\"${CARDIO_ID}\",\"price\":50.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Echocardiography\",\"category\":\"${CARDIO_ID}\",\"price\":200.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Stress Test\",\"category\":\"${CARDIO_ID}\",\"price\":150.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Holter Monitor\",\"category\":\"${CARDIO_ID}\",\"price\":180.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Cardiac Catheterization\",\"category\":\"${CARDIO_ID}\",\"price\":850.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Blood Pressure Management\",\"category\":\"${CARDIO_ID}\",\"price\":60.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"Skin Examination\",\"category\":\"${DERM_ID}\",\"price\":60.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Mole Removal\",\"category\":\"${DERM_ID}\",\"price\":120.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Acne Treatment\",\"category\":\"${DERM_ID}\",\"price\":80.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Psoriasis Treatment\",\"category\":\"${DERM_ID}\",\"price\":140.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Laser Therapy\",\"category\":\"${DERM_ID}\",\"price\":220.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Eczema Consultation\",\"category\":\"${DERM_ID}\",\"price\":75.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"Neurological Assessment\",\"category\":\"${NEURO_ID}\",\"price\":180.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"EEG Test\",\"category\":\"${NEURO_ID}\",\"price\":220.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"MRI Brain Scan\",\"category\":\"${NEURO_ID}\",\"price\":450.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Migraine Management\",\"category\":\"${NEURO_ID}\",\"price\":120.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Nerve Conduction Study\",\"category\":\"${NEURO_ID}\",\"price\":300.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"Joint Injection\",\"category\":\"${ORTHO_ID}\",\"price\":130.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Bone Density Scan\",\"category\":\"${ORTHO_ID}\",\"price\":90.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Fracture Management\",\"category\":\"${ORTHO_ID}\",\"price\":250.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Arthroscopy\",\"category\":\"${ORTHO_ID}\",\"price\":600.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Spinal Assessment\",\"category\":\"${ORTHO_ID}\",\"price\":175.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Knee Replacement Consultation\",\"category\":\"${ORTHO_ID}\",\"price\":200.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"Eye Examination\",\"category\":\"${OPHTHO_ID}\",\"price\":70.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Cataract Assessment\",\"category\":\"${OPHTHO_ID}\",\"price\":150.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Glaucoma Screening\",\"category\":\"${OPHTHO_ID}\",\"price\":95.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Retinal Scan\",\"category\":\"${OPHTHO_ID}\",\"price\":130.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Laser Eye Surgery Consultation\",\"category\":\"${OPHTHO_ID}\",\"price\":200.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"Psychiatric Evaluation\",\"category\":\"${PSYCH_ID}\",\"price\":200.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Cognitive Behavioural Therapy\",\"category\":\"${PSYCH_ID}\",\"price\":120.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Depression Assessment\",\"category\":\"${PSYCH_ID}\",\"price\":160.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Anxiety Management Session\",\"category\":\"${PSYCH_ID}\",\"price\":110.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"ADHD Assessment\",\"category\":\"${PSYCH_ID}\",\"price\":250.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"Colonoscopy\",\"category\":\"${GASTRO_ID}\",\"price\":350.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Endoscopy\",\"category\":\"${GASTRO_ID}\",\"price\":300.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"IBS Consultation\",\"category\":\"${GASTRO_ID}\",\"price\":100.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Liver Function Assessment\",\"category\":\"${GASTRO_ID}\",\"price\":130.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Nutritional Therapy\",\"category\":\"${GASTRO_ID}\",\"price\":85.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"Diabetes Management\",\"category\":\"${ENDO_ID}\",\"price\":90.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Thyroid Assessment\",\"category\":\"${ENDO_ID}\",\"price\":110.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Hormone Panel Test\",\"category\":\"${ENDO_ID}\",\"price\":140.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Insulin Therapy Consultation\",\"category\":\"${ENDO_ID}\",\"price\":100.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Osteoporosis Management\",\"category\":\"${ENDO_ID}\",\"price\":120.00}" | jq -r '"  " + .name'

post "/api/treatments" "{\"name\":\"Spirometry Test\",\"category\":\"${PULMO_ID}\",\"price\":80.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Asthma Management\",\"category\":\"${PULMO_ID}\",\"price\":95.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Sleep Apnea Assessment\",\"category\":\"${PULMO_ID}\",\"price\":200.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"COPD Consultation\",\"category\":\"${PULMO_ID}\",\"price\":130.00}" | jq -r '"  " + .name'
post "/api/treatments" "{\"name\":\"Bronchoscopy\",\"category\":\"${PULMO_ID}\",\"price\":400.00}" | jq -r '"  " + .name'

# ---- PATIENTS ----
echo ""
echo "Creating 200 patients..."

p() { post "/api/patients/create" "$1" | jq -r '"  " + .name'; }

p '{"name":"Alice Johnson","email":"alice.johnson@example.com","gender":"FEMALE","address":"12 Baker Street, London","dateOfBirth":"1985-03-14","registeredDate":"2024-01-10"}'
p '{"name":"Bob Smith","email":"bob.smith@example.com","gender":"MALE","address":"45 Oak Avenue, Manchester","dateOfBirth":"1978-07-22","registeredDate":"2024-01-15"}'
p '{"name":"Carol Davis","email":"carol.davis@example.com","gender":"FEMALE","address":"8 Elm Road, Birmingham","dateOfBirth":"1990-11-05","registeredDate":"2024-02-01"}'
p '{"name":"David Wilson","email":"david.wilson@example.com","gender":"MALE","address":"23 Pine Lane, Leeds","dateOfBirth":"1965-04-30","registeredDate":"2024-02-14"}'
p '{"name":"Emma Brown","email":"emma.brown@example.com","gender":"FEMALE","address":"67 Maple Street, Bristol","dateOfBirth":"1993-08-18","registeredDate":"2024-03-05"}'
p '{"name":"Frank Miller","email":"frank.miller@example.com","gender":"MALE","address":"3 Birch Close, Edinburgh","dateOfBirth":"1971-12-09","registeredDate":"2024-03-20"}'
p '{"name":"Grace Taylor","email":"grace.taylor@example.com","gender":"FEMALE","address":"19 Cedar Drive, Glasgow","dateOfBirth":"1988-05-27","registeredDate":"2024-04-02"}'
p '{"name":"Henry Anderson","email":"henry.anderson@example.com","gender":"MALE","address":"56 Willow Way, Sheffield","dateOfBirth":"1960-02-14","registeredDate":"2024-04-18"}'
p '{"name":"Isabella Thomas","email":"isabella.thomas@example.com","gender":"FEMALE","address":"31 Poplar Road, Liverpool","dateOfBirth":"1996-09-03","registeredDate":"2024-05-07"}'
p '{"name":"James Martinez","email":"james.martinez@example.com","gender":"MALE","address":"74 Ash Street, Nottingham","dateOfBirth":"1982-06-20","registeredDate":"2024-05-22"}'
p '{"name":"Karen White","email":"karen.white@example.com","gender":"FEMALE","address":"14 Chestnut Avenue, Cardiff","dateOfBirth":"1975-01-11","registeredDate":"2024-06-10"}'
p '{"name":"Liam Harris","email":"liam.harris@example.com","gender":"MALE","address":"38 Sycamore Close, Leicester","dateOfBirth":"2001-10-25","registeredDate":"2024-06-28"}'
p '{"name":"Mia Clark","email":"mia.clark@example.com","gender":"FEMALE","address":"9 Hawthorn Lane, Coventry","dateOfBirth":"1998-04-15","registeredDate":"2024-07-14"}'
p '{"name":"Noah Lewis","email":"noah.lewis@example.com","gender":"MALE","address":"52 Rowan Drive, Newcastle","dateOfBirth":"1969-08-07","registeredDate":"2024-08-01"}'
p '{"name":"Olivia Walker","email":"olivia.walker@example.com","gender":"FEMALE","address":"27 Alder Street, Southampton","dateOfBirth":"1984-03-29","registeredDate":"2024-08-19"}'
p '{"name":"Peter Hall","email":"peter.hall@example.com","gender":"MALE","address":"63 Beech Road, Portsmouth","dateOfBirth":"1955-11-17","registeredDate":"2024-09-05"}'
p '{"name":"Quinn Young","email":"quinn.young@example.com","gender":"FEMALE","address":"41 Holly Close, Oxford","dateOfBirth":"2000-07-04","registeredDate":"2024-09-23"}'
p '{"name":"Robert King","email":"robert.king@example.com","gender":"MALE","address":"18 Ivy Lane, Cambridge","dateOfBirth":"1973-02-28","registeredDate":"2024-10-11"}'
p '{"name":"Sofia Wright","email":"sofia.wright@example.com","gender":"FEMALE","address":"85 Laurel Street, Exeter","dateOfBirth":"1991-06-12","registeredDate":"2024-11-03"}'
p '{"name":"Thomas Green","email":"thomas.green@example.com","gender":"MALE","address":"7 Magnolia Way, Brighton","dateOfBirth":"1967-09-23","registeredDate":"2024-12-01"}'
p '{"name":"Amelia Scott","email":"amelia.scott@example.com","gender":"FEMALE","address":"29 Rosewood Avenue, Norwich","dateOfBirth":"1992-02-19","registeredDate":"2024-01-22"}'
p '{"name":"Ethan Adams","email":"ethan.adams@example.com","gender":"MALE","address":"14 Thornton Road, Plymouth","dateOfBirth":"1987-08-11","registeredDate":"2024-02-08"}'
p '{"name":"Charlotte Baker","email":"charlotte.baker@example.com","gender":"FEMALE","address":"6 Fernwood Close, Derby","dateOfBirth":"1979-05-03","registeredDate":"2024-02-25"}'
p '{"name":"Oliver Carter","email":"oliver.carter@example.com","gender":"MALE","address":"33 Meadow Lane, Swansea","dateOfBirth":"1994-12-17","registeredDate":"2024-03-12"}'
p '{"name":"Isla Mitchell","email":"isla.mitchell@example.com","gender":"FEMALE","address":"50 Riverside Drive, Aberdeen","dateOfBirth":"1983-07-08","registeredDate":"2024-03-29"}'
p '{"name":"William Turner","email":"william.turner@example.com","gender":"MALE","address":"21 Foxglove Way, Stoke-on-Trent","dateOfBirth":"1958-04-25","registeredDate":"2024-04-15"}'
p '{"name":"Evie Phillips","email":"evie.phillips@example.com","gender":"FEMALE","address":"47 Juniper Street, Hull","dateOfBirth":"1997-01-30","registeredDate":"2024-05-02"}'
p '{"name":"Harry Campbell","email":"harry.campbell@example.com","gender":"MALE","address":"11 Bluebell Court, Wolverhampton","dateOfBirth":"1976-10-14","registeredDate":"2024-05-19"}'
p '{"name":"Lily Evans","email":"lily.evans@example.com","gender":"FEMALE","address":"88 Sunflower Road, Sunderland","dateOfBirth":"2003-06-22","registeredDate":"2024-06-06"}'
p '{"name":"Jack Roberts","email":"jack.roberts@example.com","gender":"MALE","address":"16 Lavender Close, Blackpool","dateOfBirth":"1962-03-05","registeredDate":"2024-06-24"}'
p '{"name":"Freya Morgan","email":"freya.morgan@example.com","gender":"FEMALE","address":"72 Daisy Lane, Bournemouth","dateOfBirth":"1989-09-16","registeredDate":"2024-07-11"}'
p '{"name":"Charlie Hughes","email":"charlie.hughes@example.com","gender":"MALE","address":"35 Violet Avenue, Middlesbrough","dateOfBirth":"1977-11-28","registeredDate":"2024-07-29"}'
p '{"name":"Poppy Price","email":"poppy.price@example.com","gender":"FEMALE","address":"59 Primrose Way, Ipswich","dateOfBirth":"2002-04-09","registeredDate":"2024-08-16"}'
p '{"name":"George Bennett","email":"george.bennett@example.com","gender":"MALE","address":"24 Cornflower Close, Luton","dateOfBirth":"1970-08-21","registeredDate":"2024-09-03"}'
p '{"name":"Rosie Wood","email":"rosie.wood@example.com","gender":"FEMALE","address":"43 Forget-Me-Not Road, Milton Keynes","dateOfBirth":"1986-02-14","registeredDate":"2024-09-20"}'
p '{"name":"Alfie Barnes","email":"alfie.barnes@example.com","gender":"MALE","address":"17 Dandelion Street, Northampton","dateOfBirth":"1999-07-31","registeredDate":"2024-10-07"}'
p '{"name":"Daisy Coleman","email":"daisy.coleman@example.com","gender":"FEMALE","address":"62 Buttercup Lane, Peterborough","dateOfBirth":"1974-12-03","registeredDate":"2024-10-25"}'
p '{"name":"Leo Richardson","email":"leo.richardson@example.com","gender":"MALE","address":"38 Bluebell Avenue, Reading","dateOfBirth":"1964-05-18","registeredDate":"2024-11-12"}'
p '{"name":"Ruby James","email":"ruby.james@example.com","gender":"FEMALE","address":"9 Foxglove Road, Slough","dateOfBirth":"1995-10-07","registeredDate":"2024-11-29"}'
p '{"name":"Archie Watson","email":"archie.watson@example.com","gender":"MALE","address":"55 Heather Close, Gloucester","dateOfBirth":"1981-03-26","registeredDate":"2024-12-16"}'
p '{"name":"Ellie Murray","email":"ellie.murray@example.com","gender":"FEMALE","address":"30 Thistle Lane, Dundee","dateOfBirth":"1993-06-14","registeredDate":"2024-01-05"}'
p '{"name":"Theo Harrison","email":"theo.harrison@example.com","gender":"MALE","address":"46 Bracken Road, Inverness","dateOfBirth":"1968-09-02","registeredDate":"2024-01-19"}'
p '{"name":"Abigail Foster","email":"abigail.foster@example.com","gender":"FEMALE","address":"13 Fern Drive, Perth","dateOfBirth":"1980-01-27","registeredDate":"2024-02-06"}'
p '{"name":"Max Dixon","email":"max.dixon@example.com","gender":"MALE","address":"71 Heather Way, Stirling","dateOfBirth":"2000-11-15","registeredDate":"2024-02-23"}'
p '{"name":"Phoebe Harvey","email":"phoebe.harvey@example.com","gender":"FEMALE","address":"28 Clover Close, Wigan","dateOfBirth":"1977-04-04","registeredDate":"2024-03-11"}'
p '{"name":"Oscar Graham","email":"oscar.graham@example.com","gender":"MALE","address":"84 Trefoil Avenue, Warrington","dateOfBirth":"1963-07-19","registeredDate":"2024-03-28"}'
p '{"name":"Hannah Stone","email":"hannah.stone@example.com","gender":"FEMALE","address":"5 Cloverleaf Road, Bolton","dateOfBirth":"1991-12-30","registeredDate":"2024-04-14"}'
p '{"name":"Freddie Knight","email":"freddie.knight@example.com","gender":"MALE","address":"40 Acacia Road, Oldham","dateOfBirth":"1985-05-08","registeredDate":"2024-05-01"}'
p '{"name":"Emily Fox","email":"emily.fox@example.com","gender":"FEMALE","address":"22 Birchwood Lane, Rochdale","dateOfBirth":"1972-10-23","registeredDate":"2024-05-18"}'
p '{"name":"Sebastian Wells","email":"sebastian.wells@example.com","gender":"MALE","address":"67 Ashwood Drive, Salford","dateOfBirth":"1988-02-06","registeredDate":"2024-06-04"}'
p '{"name":"Zoe Reid","email":"zoe.reid@example.com","gender":"FEMALE","address":"15 Elmwood Avenue, Preston","dateOfBirth":"1996-07-17","registeredDate":"2024-06-21"}'
p '{"name":"Benjamin Cole","email":"benjamin.cole@example.com","gender":"MALE","address":"53 Oakwood Close, Blackburn","dateOfBirth":"1957-11-09","registeredDate":"2024-07-08"}'
p '{"name":"Chloe Ward","email":"chloe.ward@example.com","gender":"FEMALE","address":"37 Maplewood Road, Burnley","dateOfBirth":"1984-04-21","registeredDate":"2024-07-25"}'
p '{"name":"Joshua Perry","email":"joshua.perry@example.com","gender":"MALE","address":"79 Pinewood Lane, Lancaster","dateOfBirth":"2001-08-13","registeredDate":"2024-08-11"}'
p '{"name":"Molly Sanders","email":"molly.sanders@example.com","gender":"FEMALE","address":"26 Cedarwood Way, Carlisle","dateOfBirth":"1969-01-28","registeredDate":"2024-08-28"}'
p '{"name":"Daniel Hughes","email":"daniel.hughes@example.com","gender":"MALE","address":"48 Pinehurst Avenue, Durham","dateOfBirth":"1975-06-10","registeredDate":"2024-09-14"}'
p '{"name":"Scarlett Long","email":"scarlett.long@example.com","gender":"FEMALE","address":"11 Elmhurst Close, Sunderland","dateOfBirth":"1998-03-25","registeredDate":"2024-10-01"}'
p '{"name":"Joseph Murphy","email":"joseph.murphy@example.com","gender":"MALE","address":"64 Oakfield Road, Gateshead","dateOfBirth":"1966-09-16","registeredDate":"2024-10-18"}'
p '{"name":"Jasmine Patterson","email":"jasmine.patterson@example.com","gender":"FEMALE","address":"33 Willowfield Lane, York","dateOfBirth":"1990-12-05","registeredDate":"2024-11-04"}'
p '{"name":"Samuel Jenkins","email":"samuel.jenkins@example.com","gender":"MALE","address":"57 Springwood Road, Harrogate","dateOfBirth":"1953-07-29","registeredDate":"2024-11-21"}'
p '{"name":"Layla Simmons","email":"layla.simmons@example.com","gender":"FEMALE","address":"20 Ferndale Avenue, Wakefield","dateOfBirth":"1987-05-12","registeredDate":"2024-12-08"}'
p '{"name":"Ryan Butler","email":"ryan.butler@example.com","gender":"MALE","address":"44 Clearwater Drive, Barnsley","dateOfBirth":"1979-02-24","registeredDate":"2025-01-06"}'
p '{"name":"Aria Simmons","email":"aria.simmons@example.com","gender":"FEMALE","address":"73 Woodside Close, Doncaster","dateOfBirth":"2004-06-18","registeredDate":"2025-01-20"}'
p '{"name":"Dylan Powell","email":"dylan.powell@example.com","gender":"MALE","address":"8 Greenfield Road, Rotherham","dateOfBirth":"1960-11-03","registeredDate":"2025-02-03"}'
p '{"name":"Sienna Ross","email":"sienna.ross@example.com","gender":"FEMALE","address":"36 Highfield Avenue, Chesterfield","dateOfBirth":"1994-08-29","registeredDate":"2025-02-17"}'
p '{"name":"Nathan Henderson","email":"nathan.henderson@example.com","gender":"MALE","address":"61 Moorland Road, Mansfield","dateOfBirth":"1982-04-14","registeredDate":"2025-03-03"}'
p '{"name":"Luna Coleman","email":"luna.coleman@example.com","gender":"FEMALE","address":"19 Brookside Lane, Lincoln","dateOfBirth":"1976-01-06","registeredDate":"2025-03-17"}'
p '{"name":"Adam Alexander","email":"adam.alexander@example.com","gender":"MALE","address":"82 Riverside Way, Grimsby","dateOfBirth":"1989-10-21","registeredDate":"2025-03-31"}'
p '{"name":"Violet Morrison","email":"violet.morrison@example.com","gender":"FEMALE","address":"27 Hillside Drive, Scunthorpe","dateOfBirth":"1967-03-17","registeredDate":"2025-04-14"}'
p '{"name":"Callum Russell","email":"callum.russell@example.com","gender":"MALE","address":"54 Valleyview Road, Huddersfield","dateOfBirth":"2002-07-09","registeredDate":"2025-04-28"}'
p '{"name":"Penelope Griffin","email":"penelope.griffin@example.com","gender":"FEMALE","address":"16 Crestwood Close, Halifax","dateOfBirth":"1983-12-24","registeredDate":"2025-05-12"}'
p '{"name":"Cameron Diaz","email":"cameron.diaz@example.com","gender":"MALE","address":"41 Lakeside Avenue, Bradford","dateOfBirth":"1971-09-08","registeredDate":"2025-05-26"}'
p '{"name":"Nora Fletcher","email":"nora.fletcher@example.com","gender":"FEMALE","address":"68 Meadowbrook Road, Keighley","dateOfBirth":"1995-05-31","registeredDate":"2025-06-09"}'
p '{"name":"Evan Webb","email":"evan.webb@example.com","gender":"MALE","address":"23 Streamside Close, Skipton","dateOfBirth":"1959-02-12","registeredDate":"2025-06-23"}'
p '{"name":"Aurora Hudson","email":"aurora.hudson@example.com","gender":"FEMALE","address":"77 Willowbrook Lane, Harlow","dateOfBirth":"1986-07-26","registeredDate":"2025-07-07"}'
p '{"name":"Connor Lane","email":"connor.lane@example.com","gender":"MALE","address":"34 Springbrook Avenue, Chelmsford","dateOfBirth":"1974-04-01","registeredDate":"2025-07-21"}'
p '{"name":"Eliza Grant","email":"eliza.grant@example.com","gender":"FEMALE","address":"50 Clearbrook Road, Colchester","dateOfBirth":"1999-11-14","registeredDate":"2025-08-04"}'
p '{"name":"Reuben Black","email":"reuben.black@example.com","gender":"MALE","address":"12 Fernbrook Close, Southend-on-Sea","dateOfBirth":"1956-06-27","registeredDate":"2025-08-18"}'
p '{"name":"Cora Elliott","email":"cora.elliott@example.com","gender":"FEMALE","address":"88 Woodland Avenue, Basildon","dateOfBirth":"1992-01-19","registeredDate":"2025-09-01"}'
p '{"name":"Tobias Crawford","email":"tobias.crawford@example.com","gender":"MALE","address":"45 Greenbrook Drive, Thurrock","dateOfBirth":"1978-08-04","registeredDate":"2025-09-15"}'
p '{"name":"Iris Pearce","email":"iris.pearce@example.com","gender":"FEMALE","address":"21 Oakbrook Road, Watford","dateOfBirth":"1970-03-22","registeredDate":"2025-09-29"}'
p '{"name":"Elliot Burns","email":"elliot.burns@example.com","gender":"MALE","address":"66 Cedarbrook Lane, St Albans","dateOfBirth":"1997-10-08","registeredDate":"2025-10-13"}'
p '{"name":"Maya Fleming","email":"maya.fleming@example.com","gender":"FEMALE","address":"39 Birchbrook Close, Hemel Hempstead","dateOfBirth":"1981-05-15","registeredDate":"2025-10-27"}'
p '{"name":"Marcus Shaw","email":"marcus.shaw@example.com","gender":"MALE","address":"14 Elmbrook Avenue, Stevenage","dateOfBirth":"1963-12-30","registeredDate":"2025-11-10"}'
p '{"name":"Rosalie Hunt","email":"rosalie.hunt@example.com","gender":"FEMALE","address":"58 Maplewood Close, Luton","dateOfBirth":"1988-06-03","registeredDate":"2025-11-24"}'
p '{"name":"Dominic Ward","email":"dominic.ward@example.com","gender":"MALE","address":"32 Pinewood Avenue, Bedford","dateOfBirth":"1973-09-17","registeredDate":"2025-12-08"}'
p '{"name":"Clara Stone","email":"clara.stone@example.com","gender":"FEMALE","address":"76 Ashbrook Road, Milton Keynes","dateOfBirth":"2003-02-28","registeredDate":"2025-12-22"}'
p '{"name":"Patrick Walsh","email":"patrick.walsh@example.com","gender":"MALE","address":"5 Oakfield Drive, Northampton","dateOfBirth":"1966-07-14","registeredDate":"2026-01-05"}'
p '{"name":"Vivienne Morgan","email":"vivienne.morgan@example.com","gender":"FEMALE","address":"43 Willowfield Close, Coventry","dateOfBirth":"1984-04-09","registeredDate":"2026-01-19"}'
p '{"name":"Spencer Cole","email":"spencer.cole@example.com","gender":"MALE","address":"60 Fernfield Road, Leicester","dateOfBirth":"1991-11-23","registeredDate":"2026-02-02"}'
p '{"name":"Harriet Page","email":"harriet.page@example.com","gender":"FEMALE","address":"27 Cedarfield Lane, Derby","dateOfBirth":"1977-08-31","registeredDate":"2026-02-16"}'
p '{"name":"Brendan Ross","email":"brendan.ross@example.com","gender":"MALE","address":"85 Birchfield Avenue, Nottingham","dateOfBirth":"1961-03-06","registeredDate":"2026-03-02"}'
p '{"name":"Celeste Hughes","email":"celeste.hughes@example.com","gender":"FEMALE","address":"18 Maplefield Road, Sheffield","dateOfBirth":"1996-10-19","registeredDate":"2026-03-16"}'
p '{"name":"Marcus Dixon","email":"marcus.dixon@example.com","gender":"MALE","address":"52 Pinefield Close, Leeds","dateOfBirth":"1972-05-24","registeredDate":"2026-03-30"}'
p '{"name":"Sabrina Ford","email":"sabrina.ford@example.com","gender":"FEMALE","address":"37 Ashfield Drive, Bradford","dateOfBirth":"1989-02-07","registeredDate":"2026-04-13"}'
p '{"name":"Leon Murray","email":"leon.murray@example.com","gender":"MALE","address":"74 Elmfield Avenue, Manchester","dateOfBirth":"1954-07-21","registeredDate":"2026-04-27"}'
p '{"name":"Gwendolyn Hart","email":"gwendolyn.hart@example.com","gender":"FEMALE","address":"11 Willowfield Road, Liverpool","dateOfBirth":"1980-12-14","registeredDate":"2026-05-11"}'
p '{"name":"Fletcher Knight","email":"fletcher.knight@example.com","gender":"MALE","address":"49 Fernfield Close, Chester","dateOfBirth":"2002-09-05","registeredDate":"2026-05-25"}'
p '{"name":"Beatrice Cole","email":"beatrice.cole@example.com","gender":"FEMALE","address":"22 Cedarfield Road, Warrington","dateOfBirth":"1975-06-17","registeredDate":"2026-06-08"}'
p '{"name":"Miles Carter","email":"miles.carter@example.com","gender":"MALE","address":"66 Birchfield Lane, Wigan","dateOfBirth":"1968-01-29","registeredDate":"2026-06-15"}'
p '{"name":"Genevieve Wood","email":"genevieve.wood@example.com","gender":"FEMALE","address":"38 Oakfield Close, Bolton","dateOfBirth":"1993-04-12","registeredDate":"2026-06-20"}'
p '{"name":"Alastair Pope","email":"alastair.pope@example.com","gender":"MALE","address":"55 Maplefield Avenue, Stockport","dateOfBirth":"1985-09-25","registeredDate":"2024-01-08"}'
p '{"name":"Winifred Hunt","email":"winifred.hunt@example.com","gender":"FEMALE","address":"20 Pinefield Road, Oldham","dateOfBirth":"1952-04-16","registeredDate":"2024-01-25"}'
p '{"name":"Cecil Graham","email":"cecil.graham@example.com","gender":"MALE","address":"43 Ashfield Close, Rochdale","dateOfBirth":"1979-11-30","registeredDate":"2024-02-11"}'
p '{"name":"Millicent Shaw","email":"millicent.shaw@example.com","gender":"FEMALE","address":"17 Elmfield Drive, Salford","dateOfBirth":"1987-06-05","registeredDate":"2024-02-28"}'
p '{"name":"Percival Ford","email":"percival.ford@example.com","gender":"MALE","address":"72 Willowfield Lane, Preston","dateOfBirth":"1964-01-20","registeredDate":"2024-03-16"}'
p '{"name":"Cordelia Nash","email":"cordelia.nash@example.com","gender":"FEMALE","address":"29 Birchfield Road, Blackburn","dateOfBirth":"1998-08-02","registeredDate":"2024-04-03"}'
p '{"name":"Edmund Long","email":"edmund.long@example.com","gender":"MALE","address":"61 Fernfield Avenue, Burnley","dateOfBirth":"1971-03-14","registeredDate":"2024-04-21"}'
p '{"name":"Josephine Wells","email":"josephine.wells@example.com","gender":"FEMALE","address":"14 Cedarfield Close, Lancaster","dateOfBirth":"1983-10-27","registeredDate":"2024-05-08"}'
p '{"name":"Reginald Cross","email":"reginald.cross@example.com","gender":"MALE","address":"48 Oakfield Lane, Carlisle","dateOfBirth":"1958-05-09","registeredDate":"2024-05-25"}'
p '{"name":"Agatha Mann","email":"agatha.mann@example.com","gender":"FEMALE","address":"35 Maplefield Drive, Durham","dateOfBirth":"1976-12-22","registeredDate":"2024-06-12"}'
p '{"name":"Rupert Ryan","email":"rupert.ryan@example.com","gender":"MALE","address":"83 Pinefield Avenue, York","dateOfBirth":"1992-07-17","registeredDate":"2024-06-30"}'
p '{"name":"Cecily Ward","email":"cecily.ward@example.com","gender":"FEMALE","address":"26 Ashfield Road, Harrogate","dateOfBirth":"1969-02-04","registeredDate":"2024-07-17"}'
p '{"name":"Alistair Webb","email":"alistair.webb@example.com","gender":"MALE","address":"59 Elmfield Close, Wakefield","dateOfBirth":"1956-09-11","registeredDate":"2024-08-03"}'
p '{"name":"Araminta Fox","email":"araminta.fox@example.com","gender":"FEMALE","address":"32 Willowfield Avenue, Leeds","dateOfBirth":"1990-04-26","registeredDate":"2024-08-20"}'
p '{"name":"Wallace Burns","email":"wallace.burns@example.com","gender":"MALE","address":"67 Birchfield Close, Bradford","dateOfBirth":"1974-11-08","registeredDate":"2024-09-07"}'
p '{"name":"Lavinia Dixon","email":"lavinia.dixon@example.com","gender":"FEMALE","address":"41 Cedarfield Avenue, Sheffield","dateOfBirth":"2001-06-21","registeredDate":"2024-09-24"}'
p '{"name":"Montgomery Bell","email":"montgomery.bell@example.com","gender":"MALE","address":"18 Fernfield Road, Nottingham","dateOfBirth":"1963-01-15","registeredDate":"2024-10-11"}'
p '{"name":"Octavia Ross","email":"octavia.ross@example.com","gender":"FEMALE","address":"54 Oakfield Avenue, Derby","dateOfBirth":"1986-08-28","registeredDate":"2024-10-28"}'
p '{"name":"Barnaby Powell","email":"barnaby.powell@example.com","gender":"MALE","address":"23 Maplefield Close, Leicester","dateOfBirth":"1978-05-03","registeredDate":"2024-11-14"}'
p '{"name":"Perdita Gray","email":"perdita.gray@example.com","gender":"FEMALE","address":"77 Pinefield Lane, Coventry","dateOfBirth":"1995-12-16","registeredDate":"2024-12-01"}'
p '{"name":"Sylvester Dean","email":"sylvester.dean@example.com","gender":"MALE","address":"10 Ashfield Avenue, Birmingham","dateOfBirth":"1951-07-30","registeredDate":"2024-12-18"}'
p '{"name":"Clementine Fox","email":"clementine.fox@example.com","gender":"FEMALE","address":"46 Elmfield Road, Wolverhampton","dateOfBirth":"1982-03-11","registeredDate":"2025-01-04"}'
p '{"name":"Fitzgerald Dean","email":"fitzgerald.dean@example.com","gender":"MALE","address":"69 Willowfield Close, Stoke-on-Trent","dateOfBirth":"1970-10-24","registeredDate":"2025-01-18"}'
p '{"name":"Eugenia Hall","email":"eugenia.hall@example.com","gender":"FEMALE","address":"36 Birchfield Avenue, Shrewsbury","dateOfBirth":"1988-05-06","registeredDate":"2025-02-01"}'
p '{"name":"Alistair Grant","email":"alistair.grant@example.com","gender":"MALE","address":"53 Cedarfield Road, Telford","dateOfBirth":"1967-12-19","registeredDate":"2025-02-15"}'
p '{"name":"Thomasina Black","email":"thomasina.black@example.com","gender":"FEMALE","address":"21 Fernfield Close, Worcester","dateOfBirth":"1993-07-04","registeredDate":"2025-03-01"}'
p '{"name":"Cornelius Stone","email":"cornelius.stone@example.com","gender":"MALE","address":"58 Oakfield Road, Hereford","dateOfBirth":"1980-02-15","registeredDate":"2025-03-15"}'
p '{"name":"Sophronia Page","email":"sophronia.page@example.com","gender":"FEMALE","address":"44 Maplefield Avenue, Gloucester","dateOfBirth":"1976-09-28","registeredDate":"2025-03-29"}'
p '{"name":"Thaddeus Reid","email":"thaddeus.reid@example.com","gender":"MALE","address":"87 Pinefield Close, Cheltenham","dateOfBirth":"1961-04-12","registeredDate":"2025-04-12"}'
p '{"name":"Araminta Cole","email":"araminta.cole@example.com","gender":"FEMALE","address":"15 Ashfield Lane, Bristol","dateOfBirth":"1997-11-25","registeredDate":"2025-04-26"}'
p '{"name":"Valentine Webb","email":"valentine.webb@example.com","gender":"MALE","address":"39 Elmfield Avenue, Bath","dateOfBirth":"1984-06-07","registeredDate":"2025-05-10"}'
p '{"name":"Cassandra Hunt","email":"cassandra.hunt@example.com","gender":"FEMALE","address":"62 Willowfield Road, Taunton","dateOfBirth":"1972-01-20","registeredDate":"2025-05-24"}'
p '{"name":"Bartholomew Nash","email":"bartholomew.nash@example.com","gender":"MALE","address":"28 Birchfield Close, Exeter","dateOfBirth":"1955-08-03","registeredDate":"2025-06-07"}'
p '{"name":"Lavender Shaw","email":"lavender.shaw@example.com","gender":"FEMALE","address":"75 Cedarfield Avenue, Plymouth","dateOfBirth":"1991-03-16","registeredDate":"2025-06-21"}'
p '{"name":"Archibald Burns","email":"archibald.burns@example.com","gender":"MALE","address":"33 Fernfield Road, Torquay","dateOfBirth":"1969-10-29","registeredDate":"2025-07-05"}'
p '{"name":"Seraphina Long","email":"seraphina.long@example.com","gender":"FEMALE","address":"50 Oakfield Close, Truro","dateOfBirth":"2000-05-14","registeredDate":"2025-07-19"}'
p '{"name":"Ignatius Ford","email":"ignatius.ford@example.com","gender":"MALE","address":"22 Maplefield Road, Bodmin","dateOfBirth":"1975-12-27","registeredDate":"2025-08-02"}'
p '{"name":"Wilhelmina Gray","email":"wilhelmina.gray@example.com","gender":"FEMALE","address":"68 Pinefield Avenue, Falmouth","dateOfBirth":"1987-07-10","registeredDate":"2025-08-16"}'
p '{"name":"Leopold Cross","email":"leopold.cross@example.com","gender":"MALE","address":"11 Ashfield Road, Newquay","dateOfBirth":"1960-02-22","registeredDate":"2025-08-30"}'
p '{"name":"Celestine Mann","email":"celestine.mann@example.com","gender":"FEMALE","address":"47 Elmfield Lane, Penzance","dateOfBirth":"1994-09-05","registeredDate":"2025-09-13"}'
p '{"name":"Horatio Bell","email":"horatio.bell@example.com","gender":"MALE","address":"34 Willowfield Avenue, St Ives","dateOfBirth":"1978-04-18","registeredDate":"2025-09-27"}'
p '{"name":"Sophronia Wells","email":"sophronia.wells@example.com","gender":"FEMALE","address":"71 Birchfield Road, Peterborough","dateOfBirth":"1965-11-01","registeredDate":"2025-10-11"}'
p '{"name":"Cornelius Webb","email":"cornelius.webb@example.com","gender":"MALE","address":"8 Cedarfield Close, Norwich","dateOfBirth":"1990-06-14","registeredDate":"2025-10-25"}'
p '{"name":"Peregrine Fox","email":"peregrine.fox@example.com","gender":"MALE","address":"56 Fernfield Avenue, Ipswich","dateOfBirth":"1973-01-27","registeredDate":"2025-11-08"}'
p '{"name":"Emmeline Grant","email":"emmeline.grant@example.com","gender":"FEMALE","address":"23 Oakfield Road, Colchester","dateOfBirth":"1982-08-10","registeredDate":"2025-11-22"}'
p '{"name":"Silvester Dean","email":"silvester.dean@example.com","gender":"MALE","address":"79 Maplefield Close, Southend-on-Sea","dateOfBirth":"1950-03-23","registeredDate":"2025-12-06"}'
p '{"name":"Clotilde Ross","email":"clotilde.ross@example.com","gender":"FEMALE","address":"42 Pinefield Road, Basildon","dateOfBirth":"1995-10-06","registeredDate":"2025-12-20"}'
p '{"name":"Augustine Blake","email":"augustine.blake@example.com","gender":"MALE","address":"17 Ashfield Avenue, Thurrock","dateOfBirth":"1983-05-19","registeredDate":"2026-01-03"}'
p '{"name":"Rosalind Stone","email":"rosalind.stone@example.com","gender":"FEMALE","address":"63 Elmfield Road, Watford","dateOfBirth":"1971-12-02","registeredDate":"2026-01-17"}'
p '{"name":"Peregrine Hall","email":"peregrine.hall@example.com","gender":"MALE","address":"36 Willowfield Close, St Albans","dateOfBirth":"1966-07-15","registeredDate":"2026-01-31"}'
p '{"name":"Veronica Flynn","email":"veronica.flynn@example.com","gender":"FEMALE","address":"54 Birchfield Avenue, Hemel Hempstead","dateOfBirth":"1989-02-28","registeredDate":"2026-02-14"}'
p '{"name":"Cornelius Shaw","email":"cornelius.shaw@example.com","gender":"MALE","address":"28 Cedarfield Road, Stevenage","dateOfBirth":"1977-09-11","registeredDate":"2026-02-28"}'
p '{"name":"Arabella Simmons","email":"arabella.simmons@example.com","gender":"FEMALE","address":"85 Fernfield Close, Luton","dateOfBirth":"2001-04-24","registeredDate":"2026-03-14"}'
p '{"name":"Fitzgerald Ross","email":"fitzgerald.ross@example.com","gender":"MALE","address":"19 Oakfield Avenue, Bedford","dateOfBirth":"1962-11-07","registeredDate":"2026-03-28"}'
p '{"name":"Calliope Nash","email":"calliope.nash@example.com","gender":"FEMALE","address":"47 Maplefield Road, Cambridge","dateOfBirth":"1986-06-20","registeredDate":"2026-04-11"}'
p '{"name":"Algernon Black","email":"algernon.black@example.com","gender":"MALE","address":"72 Pinefield Lane, Ely","dateOfBirth":"1974-01-03","registeredDate":"2026-04-25"}'
p '{"name":"Venetia Burns","email":"venetia.burns@example.com","gender":"FEMALE","address":"31 Ashfield Close, Huntingdon","dateOfBirth":"1993-08-16","registeredDate":"2026-05-09"}'
p '{"name":"Sheridan Pope","email":"sheridan.pope@example.com","gender":"MALE","address":"65 Elmfield Avenue, King'\''s Lynn","dateOfBirth":"1957-03-29","registeredDate":"2026-05-23"}'
p '{"name":"Araminta Dean","email":"araminta.dean@example.com","gender":"FEMALE","address":"38 Willowfield Road, Great Yarmouth","dateOfBirth":"1981-10-12","registeredDate":"2026-06-06"}'
p '{"name":"Huxley Ward","email":"huxley.ward@example.com","gender":"MALE","address":"14 Birchfield Road, Lowestoft","dateOfBirth":"1998-05-25","registeredDate":"2026-06-17"}'
p '{"name":"Cleopatra Ryan","email":"cleopatra.ryan@example.com","gender":"FEMALE","address":"58 Cedarfield Avenue, Bury St Edmunds","dateOfBirth":"1970-12-08","registeredDate":"2024-01-13"}'
p '{"name":"Lysander Webb","email":"lysander.webb@example.com","gender":"MALE","address":"26 Fernfield Road, Sudbury","dateOfBirth":"1985-07-21","registeredDate":"2024-01-30"}'
p '{"name":"Theodora Blake","email":"theodora.blake@example.com","gender":"FEMALE","address":"83 Oakfield Close, Chelmsford","dateOfBirth":"1979-02-03","registeredDate":"2024-02-17"}'
p '{"name":"Fabian Long","email":"fabian.long@example.com","gender":"MALE","address":"45 Maplefield Lane, Brentwood","dateOfBirth":"1992-09-16","registeredDate":"2024-03-05"}'
p '{"name":"Galadriel Hunt","email":"galadriel.hunt@example.com","gender":"FEMALE","address":"12 Pinefield Road, Romford","dateOfBirth":"1968-04-29","registeredDate":"2024-03-22"}'
p '{"name":"Octavian Cole","email":"octavian.cole@example.com","gender":"MALE","address":"69 Ashfield Avenue, Ilford","dateOfBirth":"1975-11-12","registeredDate":"2024-04-08"}'
p '{"name":"Isolde Morrison","email":"isolde.morrison@example.com","gender":"FEMALE","address":"34 Elmfield Close, Barking","dateOfBirth":"1996-06-25","registeredDate":"2024-04-25"}'
p '{"name":"Theron Russell","email":"theron.russell@example.com","gender":"MALE","address":"57 Willowfield Avenue, Dagenham","dateOfBirth":"1963-01-08","registeredDate":"2024-05-12"}'
p '{"name":"Florentine Nash","email":"florentine.nash@example.com","gender":"FEMALE","address":"22 Birchfield Road, Enfield","dateOfBirth":"1984-08-21","registeredDate":"2024-05-29"}'
p '{"name":"Crispin Gray","email":"crispin.gray@example.com","gender":"MALE","address":"78 Cedarfield Close, Harrow","dateOfBirth":"1972-03-04","registeredDate":"2024-06-15"}'
p '{"name":"Narcissa Fox","email":"narcissa.fox@example.com","gender":"FEMALE","address":"41 Fernfield Avenue, Wembley","dateOfBirth":"1988-10-17","registeredDate":"2024-07-02"}'
p '{"name":"Aurelius Dean","email":"aurelius.dean@example.com","gender":"MALE","address":"15 Oakfield Road, Ealing","dateOfBirth":"1953-05-30","registeredDate":"2024-07-19"}'
p '{"name":"Thessaly Ward","email":"thessaly.ward@example.com","gender":"FEMALE","address":"53 Maplefield Close, Richmond","dateOfBirth":"1997-01-13","registeredDate":"2024-08-05"}'
p '{"name":"Ignatius Ross","email":"ignatius.ross@example.com","gender":"MALE","address":"87 Pinefield Road, Kingston upon Thames","dateOfBirth":"1980-08-26","registeredDate":"2024-08-22"}'
p '{"name":"Isadora Bell","email":"isadora.bell@example.com","gender":"FEMALE","address":"30 Ashfield Lane, Croydon","dateOfBirth":"1973-03-11","registeredDate":"2024-09-09"}'
p '{"name":"Ptolemy Shaw","email":"ptolemy.shaw@example.com","gender":"MALE","address":"74 Elmfield Road, Bromley","dateOfBirth":"1967-10-24","registeredDate":"2024-09-26"}'
p '{"name":"Calypso Webb","email":"calypso.webb@example.com","gender":"FEMALE","address":"37 Willowfield Drive, Lewisham","dateOfBirth":"1999-05-07","registeredDate":"2024-10-13"}'
p '{"name":"Caspian Burns","email":"caspian.burns@example.com","gender":"MALE","address":"61 Birchfield Avenue, Greenwich","dateOfBirth":"1961-12-20","registeredDate":"2024-10-30"}'
p '{"name":"Seraphina Black","email":"seraphina.black@example.com","gender":"FEMALE","address":"24 Hazel Road, Hackney","dateOfBirth":"1994-04-03","registeredDate":"2024-11-16"}'
p '{"name":"Maximilian Reed","email":"maximilian.reed@example.com","gender":"MALE","address":"49 Walnut Avenue, Islington","dateOfBirth":"1976-09-16","registeredDate":"2024-12-03"}'
p '{"name":"Cordelia Simmons","email":"cordelia.simmons@example.com","gender":"FEMALE","address":"73 Chestnut Road, Lambeth","dateOfBirth":"1989-02-27","registeredDate":"2024-12-20"}'
p '{"name":"Alistair Cross","email":"alistair.cross@example.com","gender":"MALE","address":"16 Hawthorn Close, Southwark","dateOfBirth":"1958-07-10","registeredDate":"2025-01-06"}'
p '{"name":"Xiomara Ellis","email":"xiomara.ellis@example.com","gender":"FEMALE","address":"62 Acacia Avenue, Tower Hamlets","dateOfBirth":"1983-12-23","registeredDate":"2025-01-23"}'
p '{"name":"Rufus Carpenter","email":"rufus.carpenter@example.com","gender":"MALE","address":"31 Maple Close, Newham","dateOfBirth":"1971-05-06","registeredDate":"2025-02-09"}'
p '{"name":"Persephone Walsh","email":"persephone.walsh@example.com","gender":"FEMALE","address":"88 Sycamore Road, Redbridge","dateOfBirth":"1995-10-19","registeredDate":"2025-02-26"}'
p '{"name":"Leander Moore","email":"leander.moore@example.com","gender":"MALE","address":"45 Birch Street, Havering","dateOfBirth":"1980-03-02","registeredDate":"2025-03-15"}'
p '{"name":"Evangeline Hart","email":"evangeline.hart@example.com","gender":"FEMALE","address":"19 Elm Close, Barnet","dateOfBirth":"1968-08-15","registeredDate":"2025-04-01"}'
p '{"name":"Sylvanus King","email":"sylvanus.king@example.com","gender":"MALE","address":"56 Oak Road, Brent","dateOfBirth":"1986-01-28","registeredDate":"2025-04-18"}'
p '{"name":"Hannelore Price","email":"hannelore.price@example.com","gender":"FEMALE","address":"33 Cedar Lane, Hillingdon","dateOfBirth":"1974-06-11","registeredDate":"2025-05-05"}'
p '{"name":"Orlando Griffiths","email":"orlando.griffiths@example.com","gender":"MALE","address":"77 Pine Avenue, Ealing","dateOfBirth":"1963-11-24","registeredDate":"2025-05-22"}'
p '{"name":"Minerva Stone","email":"minerva.stone@example.com","gender":"FEMALE","address":"42 Ash Road, Hounslow","dateOfBirth":"1991-04-07","registeredDate":"2025-06-08"}'
p '{"name":"Rafferty Cole","email":"rafferty.cole@example.com","gender":"MALE","address":"14 Rowan Close, Richmond upon Thames","dateOfBirth":"1969-09-20","registeredDate":"2025-06-25"}'
p '{"name":"Blythe Henderson","email":"blythe.henderson@example.com","gender":"FEMALE","address":"68 Willow Lane, Kingston upon Thames","dateOfBirth":"1997-02-04","registeredDate":"2025-07-12"}'
p '{"name":"Peregrine Burns","email":"peregrine.burns@example.com","gender":"MALE","address":"25 Poplar Road, Merton","dateOfBirth":"1955-07-17","registeredDate":"2025-07-29"}'
p '{"name":"Delphine Murray","email":"delphine.murray@example.com","gender":"FEMALE","address":"81 Larch Avenue, Sutton","dateOfBirth":"1982-12-30","registeredDate":"2025-08-15"}'
p '{"name":"Tobias Riley","email":"tobias.riley@example.com","gender":"MALE","address":"37 Walnut Road, Kingston","dateOfBirth":"1978-07-12","registeredDate":"2025-09-01"}'
p '{"name":"Celestina Walsh","email":"celestina.walsh@example.com","gender":"FEMALE","address":"53 Chestnut Lane, Croydon","dateOfBirth":"1999-12-25","registeredDate":"2025-09-18"}'
p '{"name":"Peregrine Gray","email":"peregrine.gray@example.com","gender":"MALE","address":"29 Hazel Close, Bromley","dateOfBirth":"1987-05-08","registeredDate":"2025-10-05"}'
p '{"name":"Valentina Cruz","email":"valentina.cruz@example.com","gender":"FEMALE","address":"11 Magnolia Road, Wandsworth","dateOfBirth":"1993-03-21","registeredDate":"2025-10-22"}'

echo ""
echo "Done. 10 categories, 55 treatments, 200 patients created."
