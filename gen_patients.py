import csv, random, datetime, re, sys

FIRST_NAMES_M = [
    "James","Liam","Noah","Ethan","Harry","Jack","Oliver","Charlie","Alfie","Oscar","George","William",
    "Benjamin","Sebastian","Thomas","Archie","Henry","Logan","Leo","Mason","Max","Elijah","Hunter",
    "Carter","Dominic","Zachary","Felix","Jasper","Tobias","Hugo","Barnaby","Rowan","Ignatius","Leander",
    "Valentine","Maximilian","Thaddeus","Reginald","Florian","Peregrine","Bartholomew","Leopold","Algernon",
    "Bertram","Remus","Casimir","Percival","Archibald","Crispin","Mortimer","Caspian","Aldous","Pompey",
    "Oswald","Theron","Raffael","Aurelius","Horatio","Lysander","Piers","Tiberius","Anselm","Silvanus",
    "Leocadio","Leofric","Florentius","Cornelius","Japheth","Mordechai","Benedictus","Aloysius","Caius",
    "Alistair","Edmund","Rupert","Cormac","Seamus","Padraig","Declan","Brendan","Ciaran","Colm","Eamon",
    "Fergus","Kieran","Niall","Rory","Sean","Tadgh","Conor","Brian","Patrick","Donal","Michael","Kevin",
    "Mark","Paul","Gerard","Anthony","Francis","Raymond","Timothy","Pascal","Vincent","Alan","Karl",
    "Shane","Derek","Peter","Martin","Neil","Barry","Frank","Mattie","Tommy","Jimmy","Johnny","Billy",
    "Robert","David","John","Joseph","Christopher","Daniel","Matthew","Andrew","Ryan","Nathan","Adam",
    "Luke","Aaron","Tyler","Dylan","Cameron","Jordan","Blake","Evan","Owen","Ian","Colin","Craig",
    "Aidan","Oisin","Fionn","Cian","Darragh","Eoin","Ruairi","Cathal","Diarmuid","Lorcan","Cillian",
    "Tadhg","Donnacha","Caolan","Ronan","Padraic","Paudie","Mossy","Robbie","Gavin","Shane","Barry",
]

FIRST_NAMES_F = [
    "Sophie","Emma","Olivia","Isla","Ava","Grace","Ella","Poppy","Freya","Amelia","Harper","Lily",
    "Chloe","Millie","Ellie","Isabella","Evie","Scarlett","Charlotte","Rosie","Daisy","Sienna","Layla",
    "Penelope","Violet","Stella","Aurora","Luna","Ruby","Matilda","Phoebe","Harriet","Imogen","Leonie",
    "Aurelia","Seraphina","Cordelia","Thea","Cecily","Arabella","Rosalind","Millicent","Gwendolyn",
    "Delilah","Evangeline","Eugenia","Leonora","Isolde","Lavinia","Seraphine","Euphemia","Minerva",
    "Rosetta","Sophronia","Celestine","Thessaly","Zephyrine","Isadora","Persephone","Marietta","Fiamma",
    "Clementine","Pelagia","Serenity","Eulalia","Cosima","Meliora","Zenobia","Cordula","Xiomara",
    "Isolina","Briseis","Seraphia","Ophelia","Calixta","Palatine","Thessalonica","Calliope","Leontine",
    "Apollonia","Chrysanthemum","Selena","Anastasia","Jessamine","Philippa","Veronique","Euphrosyne",
    "Niamh","Aoife","Siobhan","Aisling","Roisin","Grainne","Sinead","Orla","Maeve","Caoimhe",
    "Ciara","Saoirse","Eimear","Clodagh","Fionnuala","Attracta","Concepta","Assumpta","Brigid","Bridie",
    "Nuala","Majella","Veronica","Imelda","Noreen","Nora","Brenda","Breda","Agnes","Geraldine",
    "Philomena","Patricia","Kathleen","Theresa","Brigid","Mary","Joan","Anne","Helen","Rita","Vera",
    "Sarah","Emily","Hannah","Megan","Rebecca","Katie","Amy","Claire","Laura","Rachel","Caroline",
    "Jennifer","Michelle","Lisa","Karen","Sandra","Denise","Yvonne","Fiona","Louise","Catherine","Marie",
    "Sorcha","Eabha","Sadhbh","Muireann","Emer","Meadhbh","Laoise","Aoibhinn","Ailbhe","Cliona",
]

LAST_NAMES = [
    "Murphy","Kelly","Walsh","Smith","O'Brien","Byrne","Ryan","O'Connor","O'Neill","Doyle","McCarthy",
    "Gallagher","O'Sullivan","Kennedy","Lynch","Murray","Quinn","Moore","McLoughlin","Carroll","Brennan",
    "Doherty","O'Reilly","Dunne","Connolly","Reilly","Regan","Burke","Griffin","Duffy","Nolan","Clarke",
    "Kavanagh","Fitzpatrick","Keenan","Ward","Higgins","Power","Flanagan","Sheridan","Boyle","Foley",
    "Moran","O'Malley","McCarthy","Farrell","Fitzgerald","Hayes","Sweeney","Keating","Delaney","Manning",
    "Hennessy","Whelan","Leahy","Breen","Coughlan","Madden","Curran","Lyons","Gorman","Mooney","Coyne",
    "Molloy","Connors","Neville","Dowd","Hickey","Cullen","Dunphy","Phelan","Tobin","Lawlor","Hughes",
    "Ward","Morris","Reid","Fox","Hanlon","Higgins","McLoughlin","Beatty","Traynor","Smyth","Harte",
    "Tracey","Cronin","Allen","Pierce","Quill","Roche","Clifford","Larkin","Cahill","Nally","Craig",
    "Rooney","Mullen","Egan","Sherlock","Jennings","Lawless","Kane","Connelly","Dunbar","Coleman",
    "Flood","Carney","Kearney","Mullane","Riordan","Quinlan","Hallinan","Sheridan","Hannigan","Keane",
    "McDonagh","Raftery","O'Connor","Hynes","O'Dwyer","Conneely","Naughton","Keogh","Burke","Moran",
    "Kelly","Quinn","Duffy","Fagan","Nolan","Reilly","Flynn","Caffrey","Hogan","McGuinness","Gallagher",
    "Flannery","Connolly","Delaney","O'Toole","Malone","Doyle","Burke","Brennan","O'Donnell","Keane",
    "Lynch","Flanagan","Feeney","Dolan","Kelly","Quinn","Duffy","Nolan","Reilly","Flynn",
    "Daly","Pearce","Mahon","Healy","Stapleton","O'Hara","Donnelly","Cross","Craig","Rooney",
    "Casey","O'Brien","Burke","Sheridan","Brennan","Ryan","Maguire","Cowan","Kennedy","Tobin",
    "Burke","Hallinan","Sheridan","Hannigan","Keane","McDonagh","Raftery","O'Brien","Hynes","Naughton",
    "Whitfield","Clarke","Wilson","Johnston","Thompson","Martin","Anderson","Taylor","Brown","Davis",
    "Harris","Evans","Jones","Williams","Thomas","Roberts","Jackson","White","Lewis","Walker",
]

STREETS = [
    "Oak Street","Elm Avenue","River Road","Castle Lane","Maple Drive","Pine Close","Birch Way",
    "Willow Park","Cedar Court","Ash Grove","Hazel Lane","Poplar Street","Sycamore Road","Chestnut Ave",
    "Beech Street","Yew Road","Holly Drive","Alder Close","Larch Avenue","Rowan Way","Lilac Street",
    "Magnolia Court","Violet Lane","Jasmine Road","Sunflower Ave","Daisy Close","Rose Drive",
    "Tulip Street","Orchid Way","Bluebell Park","Foxglove Lane","Primrose Rd","Snowdrop Ave",
    "Heather Close","Clover Drive","Iris Street","Lavender Way","Buttercup Rd","Dandelion Ave",
    "Marigold Lane","Carnation Close","Iris Drive","Peony Street","Zinnia Way","Aster Road",
    "Dahlia Close","Geranium Ave","Heliotrope Rd","Narcissus Lane","Pansy Street","Cosmos Drive",
    "Wisteria Close","Honeysuckle Rd","Sweetpea Lane","Lupin Ave","Freesia Drive","Bluebell St",
    "Lilac Road","Dahlia Way","Hydrangea Close","Chrysanthemum Ave","Verbena Rd","Camomile Lane",
    "Sage Street","Thyme Drive","Rosemary Close","Basil Ave","Mint Rd","Parsley Lane","Coriander St",
    "Dill Drive","Fennel Close","Caraway Ave","Cumin Rd","Turmeric Lane","Saffron Street","Ginger Drive",
    "Vanilla Close","Cardamom Ave","Anise Rd","Clove Lane","Nutmeg Street","Paprika Drive","Marjoram Close",
    "Oregano Ave","Cayenne Rd","Allspice Lane","Tarragon Street","Sesame Drive","Poppy Close",
    "Sunflower Rd","Daffodil Ave","Primrose Lane","Violet Street","Lavender Drive","Bluebell Close",
    "Snowdrop Ave","Foxglove Rd","Marigold Lane","Iris Drive","Jasmine Close","Daisy Ave","Rose Rd",
    "Tulip Lane","Orchid Street","Carnation Drive","Peony Close","Zinnia Ave","Aster Rd","Lupin Lane",
    "Freesia Rd","Hydrangea Lane","Verbena Street","Wisteria Drive","Honeysuckle Close","Sweetpea Ave",
    "Kestrel Ave","Starling Rd","Swallow Close","Robin Street","Sparrow Ave","Falcon Rd","Heron Close",
    "Cormorant Lane","Gannet Street","Puffin Ave","Curlew Rd","Plover Close","Lapwing Lane","Finch Drive",
    "Goldfinch Ave","Chaffinch Rd","Bullfinch Close","Linnet Street","Treecreeper Lane","Waxwing Ave",
    "Fieldfare Close","Dipper Lane","Wagtail Street","Pipit Drive","Wheatear Ave","Bunting Close",
    "Yellowhammer Rd","Redwing Lane","Jackdaw Street","Rook Drive","Jacksnipe Ave","Snipe Close",
    "Woodcock Rd","Grouse Lane","Partridge Street","Pheasant Drive","Quail Ave","Woodpigeon Close",
    "Wren Lane","Blackbird Court","Thrush Drive","Starling Close","Swallow Way","Martin Lane",
]

TOWNS = [
    "Dublin","Cork","Galway","Limerick","Waterford","Kilkenny","Sligo","Drogheda","Dundalk","Wexford",
    "Wicklow","Athlone","Carlow","Tralee","Ennis","Letterkenny","Navan","Newry","Mullingar","Tullamore",
    "Portlaoise","Killarney","Bray","Tramore","Cobh","Clonmel","Enniscorthy","Cashel","Thurles","Longford",
    "Roscommon","Cavan","Monaghan","Enniskillen","Omagh","Derry","Antrim","Lisburn","Portadown","Ballymena",
    "Coleraine","Strabane","Cookstown","Dungannon","Athy","Naas","Newbridge","Portarlington","Tullow",
    "Gorey","Arklow","Baltinglass","Blessington","Greystones","New Ross","Dungarvan","Carrick-on-Suir",
    "Tipperary","Nenagh","Roscrea","Birr","Boyle","Carrick-on-Shannon","Manorhamilton","Ballyshannon",
    "Donegal","Milford","Dunfanaghy","Falcarragh","Dungloe","Ardara","Glenties","Killybegs","Bundoran",
    "Tubbercurry","Ballina","Castlebar","Westport","Newport","Belmullet","Crossmolina","Ballymote",
    "Claremorris","Swinford","Charlestown","Enniscrone","Coolaney","Ballisodare","Ballyhaunis","Ballinrobe",
    "Clifden","Oughterard","Tuam","Athenry","Loughrea","Portumna","Ballinasloe","Gort","Kinvara",
    "Ballyvaughan","Doolin","Lisdoonvarna","Ennistymon","Milltown Malbay","Kilkee","Kilrush","Shannon",
    "Ennis","Roscrea","Thurles","Clonmel","Cashel","Tipperary","Nenagh","Birr","Portlaoise","Tullamore",
    "Mullingar","Longford","Athlone","Roscommon","Strokestown","Elphin","Ballaghaderreen","Ballinrobe",
    "Moate","Edgeworthstown","Granard","Ballymahon","Lanesborough","Drumshanbo","Mohill","Ballinamore",
    "Carrickmacross","Castleblayney","Clones","Monaghan","Cavan","Navan","Kells","Trim","Drogheda",
    "Dundalk","Ardee","Castleblayney","Omagh","Dungannon","Cookstown","Armagh","Newry","Bangor","Lisburn",
]

BLOOD_TYPES = ["A+","A-","B+","B-","O+","O-","AB+","AB-"]
INSURERS = ["VHI","Laya","Irish Life","Aviva"]

def slugify(name):
    s = name.lower().replace("'","").replace(" ",".")
    s = re.sub(r'[^a-z0-9.]', '', s)
    return s

def random_date(start, end):
    delta = end - start
    return start + datetime.timedelta(days=random.randint(0, delta.days))

dob_start = datetime.date(1960, 1, 1)
dob_end   = datetime.date(2000, 12, 31)
reg_start = datetime.date(2024, 1, 1)
reg_end   = datetime.date(2026, 7, 25)

random.seed(42)

output_path = "/home/george/Projects/patient-system/patients_import.csv"
TOTAL = 100_000

with open(output_path, "w", newline="", encoding="utf-8") as f:
    writer = csv.writer(f, quoting=csv.QUOTE_MINIMAL)
    writer.writerow(["name","email","gender","address","dateOfBirth","registeredDate",
                     "bloodType","insuranceProvider","phoneNumber","emergencyContact"])

    seen_emails = set()

    for i in range(1, TOTAL + 1):
        gender = random.choice(["MALE","FEMALE"])
        if gender == "MALE":
            first = random.choice(FIRST_NAMES_M)
            ec_first = random.choice(FIRST_NAMES_F)
        else:
            first = random.choice(FIRST_NAMES_F)
            ec_first = random.choice(FIRST_NAMES_M)

        last = random.choice(LAST_NAMES)
        ec_last = random.choice(LAST_NAMES)

        base_email = f"{slugify(first)}.{slugify(last)}@email.com"
        email = base_email
        suffix = 2
        while email in seen_emails:
            email = f"{slugify(first)}.{slugify(last)}{suffix}@email.com"
            suffix += 1
        seen_emails.add(email)

        number  = random.randint(1, 250)
        street  = random.choice(STREETS)
        town    = random.choice(TOWNS)
        address = f"{number} {street}, {town}"

        dob  = random_date(dob_start, dob_end).strftime("%Y-%m-%d")
        reg  = random_date(reg_start, reg_end).strftime("%Y-%m-%d")
        bt   = random.choice(BLOOD_TYPES)
        ins  = random.choice(INSURERS)
        phone = f"+353851{i:06d}"
        ec   = f"{ec_first} {ec_last}"

        writer.writerow([f"{first} {last}", email, gender, address, dob, reg, bt, ins, phone, ec])

print(f"Done — {TOTAL} rows written to {output_path}")
