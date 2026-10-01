import os
import sys
import threading
import time
from fastapi import FastAPI, HTTPException, Depends
from sqlalchemy import create_engine, Column, Integer, String, Double, ForeignKey, Text, UniqueConstraint
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from pydantic import BaseModel
from typing import List, Optional

# Configuración de la Base de Datos MySQL (XAMPP)
# Reemplaza 'root' y '' si tienes contraseña en MySQL
DATABASE_URL = "mysql+mysqlconnector://root:@localhost/aula_virtual"

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# --- Modelos SQLAlchemy ---

class UserDB(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    carnet = Column(String(10), unique=True, nullable=False)
    name = Column(String(255), nullable=False)
    password = Column(String(255), nullable=False)
    role = Column(String(50), nullable=False)
    faculty = Column(String(255))
    address = Column(Text)
    personal_email = Column(String(255))
    profile_image = Column(Text)
    can_change_photo = Column(Integer, default=1)

class SubjectDB(Base):
    __tablename__ = "subjects"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False)
    description = Column(Text)
    faculty = Column(String(255))
    professorId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    section = Column(String(10), default="01")
    color = Column(String(50), default="BLUE")

class FacultyDB(Base):
    __tablename__ = "faculties"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False)
    description = Column(Text)

class EnrollmentDB(Base):
    __tablename__ = "enrollments"
    id = Column(Integer, primary_key=True, index=True)
    studentId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"))
    grade1 = Column(Double)
    grade2 = Column(Double)
    grade3 = Column(Double)
    grade4 = Column(Double)
    grade5 = Column(Double)
    color = Column(String(50), default="BLUE")
    __table_args__ = (UniqueConstraint('studentId', 'subjectId', name='_student_subject_uc'),)

class UserSubjectColorDB(Base):
    __tablename__ = "user_subject_colors"
    id = Column(Integer, primary_key=True, index=True)
    userId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"))
    color = Column(String(50), default="BLUE")
    __table_args__ = (UniqueConstraint('userId', 'subjectId', name='_user_subject_color_uc'),)

class BlogEntryDB(Base):
    __tablename__ = "blog_entries"
    id = Column(Integer, primary_key=True, index=True)
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"))
    category = Column(String(50)) # Parcial 1-5, Tarea, Info
    title = Column(String(255), nullable=False)
    content = Column(Text)
    position = Column(Integer, default=0)

class BlogCommentDB(Base):
    __tablename__ = "blog_comments"
    id = Column(Integer, primary_key=True, index=True)
    blogEntryId = Column(Integer, ForeignKey("blog_entries.id", ondelete="CASCADE"))
    userId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    content = Column(Text, nullable=False)
    timestamp = Column(String(50)) # Simplificado a string para el ejercicio

class FacilityDB(Base):
    __tablename__ = "facilities"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False)
    type = Column(String(100))
    description = Column(Text)

class FacilityScheduleDB(Base):
    __tablename__ = "facility_schedules"
    id = Column(Integer, primary_key=True, index=True)
    facilityId = Column(Integer, ForeignKey("facilities.id", ondelete="CASCADE"))
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"))
    professorId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    days = Column(String(255))
    startTime = Column(String(10))
    endTime = Column(String(10))
    color = Column(String(50), default="BLUE")

class NotificationDB(Base):
    __tablename__ = "notifications"
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(255), nullable=False)
    message = Column(Text, nullable=False)
    targetType = Column(String(50), nullable=False)
    targetValue = Column(String(255), nullable=True)
    senderName = Column(String(255), nullable=False)
    timestamp = Column(String(100), nullable=False)

# Crear tablas si no existen
Base.metadata.create_all(bind=engine)

def seed_initial_data():
    try:
        db = SessionLocal()
        if not db.query(FacultyDB).filter(FacultyDB.id == 998).first():
            db.add(FacultyDB(id=998, name="Docencia", description="Facultad obligatoria asignada automáticamente a todos los Profesores."))
        if not db.query(FacultyDB).filter(FacultyDB.id == 999).first():
            db.add(FacultyDB(id=999, name="Administrativa", description="Facultad obligatoria asignada automáticamente a todos los Administradores."))
        if not db.query(UserDB).filter(UserDB.carnet == "ADMIN12345").first():
            db.add(UserDB(carnet="ADMIN12345", name="Admin Maestro", password="ASD###", role="ADMIN", faculty="Administrativa"))
        db.commit()
        db.close()
    except Exception as e:
        print(f"Error seeding initial data in backend: {e}")

seed_initial_data()

# --- Esquemas Pydantic ---

class UserBase(BaseModel):
    carnet: str
    name: str
    password: str
    role: str
    faculty: Optional[str] = None
    address: Optional[str] = None
    personal_email: Optional[str] = None
    profile_image: Optional[str] = None
    can_change_photo: Optional[int] = 1

class User(UserBase):
    id: int
    class Config:
        from_attributes = True

class SubjectBase(BaseModel):
    name: str
    description: Optional[str] = None
    faculty: Optional[str] = None
    professorId: Optional[int] = None
    section: Optional[str] = "01"
    color: Optional[str] = "BLUE"

class Subject(SubjectBase):
    id: int
    class Config:
        from_attributes = True

class FacultyBase(BaseModel):
    name: str
    description: Optional[str] = None

class Faculty(FacultyBase):
    id: int
    class Config:
        from_attributes = True

class EnrollmentBase(BaseModel):
    studentId: int
    subjectId: int
    grade1: Optional[float] = None
    grade2: Optional[float] = None
    grade3: Optional[float] = None
    grade4: Optional[float] = None
    grade5: Optional[float] = None
    color: Optional[str] = "BLUE"

class Enrollment(EnrollmentBase):
    id: int
    class Config:
        from_attributes = True

class UserSubjectColorBase(BaseModel):
    userId: int
    subjectId: int
    color: str = "BLUE"

class UserSubjectColor(UserSubjectColorBase):
    id: int
    class Config:
        from_attributes = True

class BlogEntryBase(BaseModel):
    subjectId: int
    category: str
    title: str
    content: Optional[str] = None
    position: Optional[int] = 0

class BlogEntry(BlogEntryBase):
    id: int
    class Config:
        from_attributes = True

class FacilityBase(BaseModel):
    name: str
    type: Optional[str] = None
    description: Optional[str] = None

class Facility(FacilityBase):
    id: int
    class Config:
        from_attributes = True

class FacilityScheduleBase(BaseModel):
    facilityId: int
    subjectId: int
    professorId: int
    days: str
    startTime: str
    endTime: str
    color: Optional[str] = "BLUE"

class FacilitySchedule(FacilityScheduleBase):
    id: int
    class Config:
        from_attributes = True

class ScheduleInfo(BaseModel):
    schedule: FacilitySchedule
    subjectName: str
    subjectColor: str
    professorName: str

class BlogCommentBase(BaseModel):
    blogEntryId: int
    userId: int
    content: str
    timestamp: Optional[str] = None

class BlogComment(BlogCommentBase):
    id: int
    class Config:
        from_attributes = True

class CommentInfo(BaseModel):
    comment: BlogComment
    userName: str
    userRole: str
    userPhoto: Optional[str] = None

class NotificationBase(BaseModel):
    title: str
    message: str
    targetType: str
    targetValue: Optional[str] = None
    senderName: str
    timestamp: str

class NotificationSchema(NotificationBase):
    id: int
    class Config:
        from_attributes = True

# --- API FastAPI ---

app = FastAPI(title="Aula Virtual API")

# Dependencia para obtener la sesión
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# --- Endpoints ---

@app.get("/")
def root():
    return {
        "app": "Aula Virtual API",
        "status": "online",
        "docs": "/docs"
    }

@app.get("/users/students", response_model=List[User])
def get_students(db: Session = Depends(get_db)):
    return db.query(UserDB).filter(UserDB.role == "STUDENT").all()

@app.get("/users/admins", response_model=List[User])
def get_admins(db: Session = Depends(get_db)):
    return db.query(UserDB).filter(UserDB.role == "ADMIN").all()

@app.get("/users/professors", response_model=List[User])
def get_professors(db: Session = Depends(get_db)):
    return db.query(UserDB).filter(UserDB.role == "PROFESSOR").all()

# --- User-specific Schedules ---
@app.get("/users/student/{student_id}/schedules", response_model=List[ScheduleInfo])
def get_student_timetable(student_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .join(EnrollmentDB, SubjectDB.id == EnrollmentDB.subjectId)\
        .filter(EnrollmentDB.studentId == student_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectColor": s.color, "professorName": u.name} for sch, s, u in results]

@app.get("/users/professor/{professor_id}/schedules", response_model=List[ScheduleInfo])
def get_professor_timetable(professor_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .filter(FacilityScheduleDB.professorId == professor_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectColor": s.color, "professorName": u.name} for sch, s, u in results]

# --- User-specific Schedules ---
@app.get("/users/student/{student_id}/schedules", response_model=List[ScheduleInfo])
def get_student_timetable(student_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .join(EnrollmentDB, SubjectDB.id == EnrollmentDB.subjectId)\
        .filter(EnrollmentDB.studentId == student_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectColor": s.color, "professorName": u.name} for sch, s, u in results]

@app.get("/users/professor/{professor_id}/schedules", response_model=List[ScheduleInfo])
def get_professor_timetable(professor_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .filter(FacilityScheduleDB.professorId == professor_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectColor": s.color, "professorName": u.name} for sch, s, u in results]

@app.post("/users", response_model=User)
def create_user(user: UserBase, db: Session = Depends(get_db)):
    db_user = UserDB(**user.dict())
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

@app.put("/users/{user_id}", response_model=User)
def update_user(user_id: int, user: UserBase, db: Session = Depends(get_db)):
    db_user = db.query(UserDB).filter(UserDB.id == user_id).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    for key, value in user.dict().items():
        setattr(db_user, key, value)
    db.commit()
    db.refresh(db_user)
    return db_user

@app.delete("/users/{user_id}")
def delete_user(user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(UserDB).filter(UserDB.id == user_id).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    db.delete(db_user)
    db.commit()
    return {"message": "Usuario eliminado"}

@app.get("/users/{user_id}", response_model=User)
def get_user_by_id(user_id: int, db: Session = Depends(get_db)):
    user = db.query(UserDB).filter(UserDB.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    return user

@app.get("/login")
def login(carnet: str, password: str, db: Session = Depends(get_db)):
    user = db.query(UserDB).filter(UserDB.carnet == carnet, UserDB.password == password).first()
    if not user:
        raise HTTPException(status_code=401, detail="Credenciales inválidas")
    return user

# --- Subjects ---
@app.get("/subjects", response_model=List[Subject])
def get_subjects(db: Session = Depends(get_db)):
    return db.query(SubjectDB).all()

@app.post("/subjects", response_model=Subject)
def create_subject(subject: SubjectBase, db: Session = Depends(get_db)):
    db_sub = SubjectDB(**subject.dict())
    db.add(db_sub)
    db.commit()
    db.refresh(db_sub)
    return db_sub

@app.put("/subjects/{sub_id}", response_model=Subject)
def update_subject(sub_id: int, subject: SubjectBase, db: Session = Depends(get_db)):
    db_sub = db.query(SubjectDB).filter(SubjectDB.id == sub_id).first()
    if not db_sub:
        raise HTTPException(status_code=404, detail="Materia no encontrada")
    for key, value in subject.dict().items():
        setattr(db_sub, key, value)
    db.commit()
    db.refresh(db_sub)
    return db_sub

@app.delete("/subjects/{sub_id}")
def delete_subject(sub_id: int, db: Session = Depends(get_db)):
    db_sub = db.query(SubjectDB).filter(SubjectDB.id == sub_id).first()
    if not db_sub:
        raise HTTPException(status_code=404, detail="Materia no encontrada")
    db.delete(db_sub)
    db.commit()
    return {"message": "Materia eliminada"}

@app.get("/subjects/{sub_id}", response_model=Subject)
def get_subject_by_id(sub_id: int, db: Session = Depends(get_db)):
    sub = db.query(SubjectDB).filter(SubjectDB.id == sub_id).first()
    if not sub:
        raise HTTPException(status_code=404, detail="Materia no encontrada")
    return sub

@app.get("/subjects/professor/{prof_id}", response_model=List[Subject])
def get_subjects_by_professor(prof_id: int, db: Session = Depends(get_db)):
    return db.query(SubjectDB).join(FacilityScheduleDB, SubjectDB.id == FacilityScheduleDB.subjectId).filter(FacilityScheduleDB.professorId == prof_id).distinct().all()

# --- Faculties ---
@app.get("/faculties", response_model=List[Faculty])
def get_faculties(db: Session = Depends(get_db)):
    return db.query(FacultyDB).all()

@app.post("/faculties", response_model=Faculty)
def create_faculty(faculty: FacultyBase, db: Session = Depends(get_db)):
    db_fac = FacultyDB(**faculty.dict())
    db.add(db_fac)
    db.commit()
    db.refresh(db_fac)
    return db_fac

@app.put("/faculties/{fac_id}", response_model=Faculty)
def update_faculty(fac_id: int, faculty: FacultyBase, db: Session = Depends(get_db)):
    db_fac = db.query(FacultyDB).filter(FacultyDB.id == fac_id).first()
    if not db_fac:
        raise HTTPException(status_code=404, detail="Facultad no encontrada")

    old_name = db_fac.name
    new_name = faculty.name

    # Update the faculty itself
    for key, value in faculty.dict().items():
        setattr(db_fac, key, value)

    # Cascade update to users and subjects if name changed
    if old_name != new_name:
        db.query(UserDB).filter(UserDB.faculty == old_name).update({UserDB.faculty: new_name})
        db.query(SubjectDB).filter(SubjectDB.faculty == old_name).update({SubjectDB.faculty: new_name})

    db.commit()
    db.refresh(db_fac)
    return db_fac

@app.delete("/faculties/{fac_id}")
def delete_faculty(fac_id: int, db: Session = Depends(get_db)):
    db_fac = db.query(FacultyDB).filter(FacultyDB.id == fac_id).first()
    if not db_fac:
        raise HTTPException(status_code=404, detail="Facultad no encontrada")

    old_name = db_fac.name

    # Set faculty to None for associated users and subjects
    db.query(UserDB).filter(UserDB.faculty == old_name).update({UserDB.faculty: None})
    db.query(SubjectDB).filter(SubjectDB.faculty == old_name).update({SubjectDB.faculty: None})

    db.delete(db_fac)
    db.commit()
    return {"message": "Facultad eliminada y referencias limpiadas"}

@app.get("/faculties/{fac_id}", response_model=Faculty)
def get_faculty_by_id(fac_id: int, db: Session = Depends(get_db)):
    fac = db.query(FacultyDB).filter(FacultyDB.id == fac_id).first()
    if not fac:
        raise HTTPException(status_code=404, detail="Facultad no encontrada")
    return fac

# --- Enrollments ---
class StudentGradeInfo(BaseModel):
    enrollment: Enrollment
    subject: Subject

class StudentInSubjectInfo(BaseModel):
    enrollment: Enrollment
    student: User

@app.get("/subjects/{sub_id}/enrollments", response_model=List[StudentInSubjectInfo])
def get_subject_enrollments(sub_id: int, db: Session = Depends(get_db)):
    results = db.query(EnrollmentDB, UserDB).join(UserDB, EnrollmentDB.studentId == UserDB.id).filter(EnrollmentDB.subjectId == sub_id).all()
    return [{"enrollment": e, "student": s} for e, s in results]

@app.get("/enrollments/student/{student_id}", response_model=List[StudentGradeInfo])
def get_student_grades(student_id: int, db: Session = Depends(get_db)):
    results = db.query(EnrollmentDB, SubjectDB).join(SubjectDB, EnrollmentDB.subjectId == SubjectDB.id).filter(EnrollmentDB.studentId == student_id).all()
    return [{"enrollment": e, "subject": s} for e, s in results]

@app.post("/enrollments", response_model=Enrollment)
def enroll_student(enrollment: EnrollmentBase, db: Session = Depends(get_db)):
    # Verificar si ya existe la inscripción
    existing = db.query(EnrollmentDB).filter(
        EnrollmentDB.studentId == enrollment.studentId,
        EnrollmentDB.subjectId == enrollment.subjectId
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="El alumno ya está inscrito en esta materia")

    db_en = EnrollmentDB(**enrollment.dict())
    db.add(db_en)
    db.commit()
    db.refresh(db_en)
    return db_en

@app.put("/enrollments/{en_id}", response_model=Enrollment)
def update_enrollment(en_id: int, enrollment: EnrollmentBase, db: Session = Depends(get_db)):
    db_en = db.query(EnrollmentDB).filter(EnrollmentDB.id == en_id).first()
    if not db_en:
        raise HTTPException(status_code=404, detail="Inscripción no encontrada")
    for key, value in enrollment.dict().items():
        setattr(db_en, key, value)
    db.commit()
    db.refresh(db_en)
    return db_en

@app.delete("/enrollments/{en_id}")
def delete_enrollment(en_id: int, db: Session = Depends(get_db)):
    db_en = db.query(EnrollmentDB).filter(EnrollmentDB.id == en_id).first()
    if not db_en:
        raise HTTPException(status_code=404, detail="Inscripción no encontrada")
    db.delete(db_en)
    db.commit()
    return {"message": "Inscripción eliminada"}

@app.get("/enrollments/{en_id}", response_model=Enrollment)
def get_enrollment_by_id(en_id: int, db: Session = Depends(get_db)):
    en = db.query(EnrollmentDB).filter(EnrollmentDB.id == en_id).first()
    if not en:
        raise HTTPException(status_code=404, detail="Inscripción no encontrada")
    return en

# --- Blog Entries ---
@app.get("/subjects/{sub_id}/blog", response_model=List[BlogEntry])
def get_subject_blog(sub_id: int, db: Session = Depends(get_db)):
    try:
        return db.query(BlogEntryDB).filter(BlogEntryDB.subjectId == sub_id).order_by(BlogEntryDB.position.asc()).all()
    except Exception as e:
        print(f"Error al obtener blog: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")

@app.post("/blog", response_model=BlogEntry)
def create_blog_entry(entry: BlogEntryBase, db: Session = Depends(get_db)):
    try:
        # Find minimum position to put new entry at top
        min_pos = db.query(BlogEntryDB).filter(BlogEntryDB.subjectId == entry.subjectId).order_by(BlogEntryDB.position.asc()).first()
        new_pos = (min_pos.position - 1) if min_pos else 0

        db_entry = BlogEntryDB(**entry.dict())
        db_entry.position = new_pos
        db.add(db_entry)
        db.commit()
        db.refresh(db_entry)
        return db_entry
    except Exception as e:
        db.rollback()
        print(f"Error al crear blog: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")

@app.put("/blog/reorder")
def reorder_blog(entries: List[BlogEntry], db: Session = Depends(get_db)):
    try:
        for idx, entry in enumerate(entries):
            db.query(BlogEntryDB).filter(BlogEntryDB.id == entry.id).update({BlogEntryDB.position: idx})
        db.commit()
        return {"message": "Orden actualizado"}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@app.put("/blog/{entry_id}", response_model=BlogEntry)
def update_blog_entry(entry_id: int, entry: BlogEntryBase, db: Session = Depends(get_db)):
    db_entry = db.query(BlogEntryDB).filter(BlogEntryDB.id == entry_id).first()
    if not db_entry:
        raise HTTPException(status_code=404, detail="Entrada no encontrada")
    for key, value in entry.dict().items():
        setattr(db_entry, key, value)
    db.commit()
    db.refresh(db_entry)
    return db_entry

@app.delete("/blog/{entry_id}")
def delete_blog_entry(entry_id: int, db: Session = Depends(get_db)):
    db_entry = db.query(BlogEntryDB).filter(BlogEntryDB.id == entry_id).first()
    if not db_entry:
        raise HTTPException(status_code=404, detail="Entrada no encontrada")
    db.delete(db_entry)
    db.commit()
    return {"message": "Entrada eliminada"}

# --- Blog Comments ---
@app.get("/blog/{entry_id}/comments", response_model=List[CommentInfo])
def get_blog_comments(entry_id: int, db: Session = Depends(get_db)):
    results = db.query(BlogCommentDB, UserDB).join(UserDB, BlogCommentDB.userId == UserDB.id).filter(BlogCommentDB.blogEntryId == entry_id).order_by(BlogCommentDB.id.asc()).all()
    return [{"comment": c, "userName": u.name, "userRole": u.role, "userPhoto": u.profile_image} for c, u in results]

@app.post("/blog/comments", response_model=BlogComment)
def create_blog_comment(comment: BlogCommentBase, db: Session = Depends(get_db)):
    db_comment = BlogCommentDB(**comment.dict())
    if not db_comment.timestamp:
        from datetime import datetime
        db_comment.timestamp = datetime.now().strftime("%d/%m/%Y %I:%M %p")
    db.add(db_comment)
    db.commit()
    db.refresh(db_comment)
    return db_comment

@app.delete("/blog/comments/{comment_id}")
def delete_blog_comment(comment_id: int, db: Session = Depends(get_db)):
    db_comment = db.query(BlogCommentDB).filter(BlogCommentDB.id == comment_id).first()
    if not db_comment:
        raise HTTPException(status_code=404, detail="Comentario no encontrado")
    db.delete(db_comment)
    db.commit()
    return {"message": "Comentario eliminado"}

# --- Facilities ---
@app.get("/facilities", response_model=List[Facility])
def get_facilities(db: Session = Depends(get_db)):
    return db.query(FacilityDB).all()

@app.post("/facilities", response_model=Facility)
def create_facility(facility: FacilityBase, db: Session = Depends(get_db)):
    db_fac = FacilityDB(**facility.dict())
    db.add(db_fac)
    db.commit()
    db.refresh(db_fac)
    return db_fac

@app.put("/facilities/{fac_id}", response_model=Facility)
def update_facility(fac_id: int, facility: FacilityBase, db: Session = Depends(get_db)):
    db_fac = db.query(FacilityDB).filter(FacilityDB.id == fac_id).first()
    if not db_fac:
        raise HTTPException(status_code=404, detail="Instalación no encontrada")
    for key, value in facility.dict().items():
        setattr(db_fac, key, value)
    db.commit()
    db.refresh(db_fac)
    return db_fac

@app.delete("/facilities/{fac_id}")
def delete_facility(fac_id: int, db: Session = Depends(get_db)):
    db_fac = db.query(FacilityDB).filter(FacilityDB.id == fac_id).first()
    if not db_fac:
        raise HTTPException(status_code=404, detail="Instalación no encontrada")
    db.delete(db_fac)
    db.commit()
    return {"message": "Instalación eliminada"}

# --- Facility Schedules ---
@app.get("/facilities/{fac_id}/schedules", response_model=List[ScheduleInfo])
def get_facility_schedules(fac_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .filter(FacilityScheduleDB.facilityId == fac_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectColor": sch.color if sch.color else (s.color if s.color else "BLUE"), "professorName": u.name} for sch, s, u in results]

def time_to_minutes(time_str: str) -> int:
    try:
        parts = time_str.strip().split()
        time_parts = parts[0].split(':')
        hour = int(time_parts[0])
        minute = int(time_parts[1])
        if len(parts) > 1:
            ampm = parts[1].upper()
            if ampm == "PM" and hour < 12:
                hour += 12
            elif ampm == "AM" and hour == 12:
                hour = 0
        return hour * 60 + minute
    except Exception:
        return 0

def check_schedule_conflicts_backend(schedule: FacilityScheduleBase, db: Session, current_id: Optional[int] = None):
    new_start = time_to_minutes(schedule.startTime)
    new_end = time_to_minutes(schedule.endTime)
    if new_start >= new_end:
        raise HTTPException(status_code=400, detail="La hora de inicio debe ser menor a la hora de finalización.")

    # Normalizar días removiendo espacios extras
    new_days = [d.strip() for d in schedule.days.replace(', ', ',').split(',')]

    query = db.query(FacilityScheduleDB).filter(
        (FacilityScheduleDB.facilityId == schedule.facilityId) |
        (FacilityScheduleDB.professorId == schedule.professorId)
    )
    if current_id:
        query = query.filter(FacilityScheduleDB.id != current_id)

    for ex in query.all():
        ex_days = [d.strip() for d in ex.days.replace(', ', ',').split(',')]
        if any(d in ex_days for d in new_days):
            ex_start = time_to_minutes(ex.startTime)
            ex_end = time_to_minutes(ex.endTime)
            if new_start < ex_end and new_end > ex_start:
                if ex.facilityId == schedule.facilityId:
                    raise HTTPException(status_code=400, detail="Esta aula ya está ocupada en los días y rango de horario solicitados.")
                else:
                    raise HTTPException(status_code=400, detail="El profesor ya tiene un horario asignado en este mismo intervalo.")

@app.post("/facilities/schedules", response_model=FacilitySchedule)
def create_schedule(schedule: FacilityScheduleBase, db: Session = Depends(get_db)):
    check_schedule_conflicts_backend(schedule, db)
    db_sch = FacilityScheduleDB(**schedule.dict())
    db.add(db_sch)
    db.commit()
    db.refresh(db_sch)
    return db_sch

@app.put("/facilities/schedules/{sch_id}", response_model=FacilitySchedule)
def update_schedule(sch_id: int, schedule: FacilityScheduleBase, db: Session = Depends(get_db)):
    check_schedule_conflicts_backend(schedule, db, current_id=sch_id)
    db_sch = db.query(FacilityScheduleDB).filter(FacilityScheduleDB.id == sch_id).first()
    if not db_sch:
        raise HTTPException(status_code=404, detail="Horario no encontrado")
    for key, value in schedule.dict().items():
        setattr(db_sch, key, value)
    db.commit()
    db.refresh(db_sch)
    return db_sch

@app.delete("/facilities/schedules/{sch_id}")
def delete_schedule(sch_id: int, db: Session = Depends(get_db)):
    db_sch = db.query(FacilityScheduleDB).filter(FacilityScheduleDB.id == sch_id).first()
    if not db_sch:
        raise HTTPException(status_code=404, detail="Horario no encontrado")
    db.delete(db_sch)
    db.commit()
    return {"message": "Horario eliminado"}

@app.get("/facilities/schedules/{sch_id}")
def get_schedule_by_id(sch_id: int, db: Session = Depends(get_db)):
    sch = db.query(FacilityScheduleDB).filter(FacilityScheduleDB.id == sch_id).first()
    if not sch:
        raise HTTPException(status_code=404, detail="Horario no encontrado")
    return sch

# --- User specific schedules & subject colors ---
@app.put("/users/{user_id}/subjects/{subject_id}/color")
def update_user_subject_color(user_id: int, subject_id: int, color: str, db: Session = Depends(get_db)):
    usc = db.query(UserSubjectColorDB).filter(UserSubjectColorDB.userId == user_id, UserSubjectColorDB.subjectId == subject_id).first()
    if not usc:
        usc = UserSubjectColorDB(userId=user_id, subjectId=subject_id, color=color)
        db.add(usc)
    else:
        usc.color = color

    enrollment = db.query(EnrollmentDB).filter(EnrollmentDB.studentId == user_id, EnrollmentDB.subjectId == subject_id).first()
    if enrollment:
        enrollment.color = color

    db.commit()
    return {"message": "Color actualizado", "color": color}

@app.get("/users/{user_id}/subjects/{subject_id}/color")
def get_user_subject_color(user_id: int, subject_id: int, db: Session = Depends(get_db)):
    usc = db.query(UserSubjectColorDB).filter(UserSubjectColorDB.userId == user_id, UserSubjectColorDB.subjectId == subject_id).first()
    if usc and usc.color:
        return {"color": usc.color}
    enrollment = db.query(EnrollmentDB).filter(EnrollmentDB.studentId == user_id, EnrollmentDB.subjectId == subject_id).first()
    if enrollment and enrollment.color:
        return {"color": enrollment.color}
    subject = db.query(SubjectDB).filter(SubjectDB.id == subject_id).first()
    return {"color": subject.color if subject and subject.color else "BLUE"}

@app.get("/users/student/{student_id}/schedules", response_model=List[ScheduleInfo])
def get_student_personal_schedules(student_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB, EnrollmentDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .join(EnrollmentDB, SubjectDB.id == EnrollmentDB.subjectId)\
        .filter(EnrollmentDB.studentId == student_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectColor": e.color if e.color else (s.color if s.color else "BLUE"), "professorName": u.name} for sch, s, u, e in results]

@app.get("/users/professor/{prof_id}/schedules", response_model=List[ScheduleInfo])
def get_professor_personal_schedules(prof_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .filter(FacilityScheduleDB.professorId == prof_id).all()

    output = []
    for sch, s, u in results:
        usc = db.query(UserSubjectColorDB).filter(UserSubjectColorDB.userId == prof_id, UserSubjectColorDB.subjectId == s.id).first()
        color = usc.color if (usc and usc.color) else (s.color if s.color else "BLUE")
        output.append({"schedule": sch, "subjectName": s.name, "subjectColor": color, "professorName": u.name})
    return output

@app.delete("/blog/entries/{entry_id}/comments")
def clear_blog_discussion(entry_id: int, db: Session = Depends(get_db)):
    db.query(BlogCommentDB).filter(BlogCommentDB.blogEntryId == entry_id).delete()
    db.commit()
    return {"message": "Conversación vaciada"}

# --- Notifications ---
@app.get("/notifications", response_model=List[NotificationSchema])
def get_notifications(db: Session = Depends(get_db)):
    return db.query(NotificationDB).order_by(NotificationDB.id.desc()).all()

@app.post("/notifications", response_model=NotificationSchema)
def create_notification(notification: NotificationBase, db: Session = Depends(get_db)):
    from datetime import datetime

    # 1. Enforce Server Time 2-Hour Limit for SUPPORT Messages
    if notification.targetType == "SUPPORT":
        last_support = db.query(NotificationDB).filter(
            NotificationDB.targetType == "SUPPORT",
            NotificationDB.senderName == notification.senderName
        ).order_by(NotificationDB.id.desc()).first()

        if last_support and last_support.timestamp:
            try:
                last_dt = datetime.strptime(last_support.timestamp, "%d/%m/%Y %H:%M")
                now_dt = datetime.now()
                elapsed_seconds = (now_dt - last_dt).total_seconds()
                if elapsed_seconds < 7200:
                    remaining_mins = int((7200 - elapsed_seconds) / 60)
                    hours = remaining_mins // 60
                    mins = remaining_mins % 60
                    time_msg = f"{hours} hora(s) y {mins} minuto(s)" if hours > 0 else f"{mins} minuto(s)"
                    raise HTTPException(status_code=400, detail=f"Debes esperar {time_msg} antes de enviar otro reporte a los Administradores.")
            except ValueError:
                pass

    # 2. Always stamp exact server time
    notif_data = notification.dict()
    notif_data["timestamp"] = datetime.now().strftime("%d/%m/%Y %H:%M")

    db_notif = NotificationDB(**notif_data)
    db.add(db_notif)
    db.commit()
    db.refresh(db_notif)
    return db_notif

@app.put("/notifications/{id}", response_model=NotificationSchema)
def update_notification(id: int, notification: NotificationBase, db: Session = Depends(get_db)):
    db_notif = db.query(NotificationDB).filter(NotificationDB.id == id).first()
    if not db_notif:
        raise HTTPException(status_code=404, detail="Notificación no encontrada")
    for key, value in notification.dict().items():
        setattr(db_notif, key, value)
    db.commit()
    db.refresh(db_notif)
    return db_notif

@app.delete("/notifications/{id}")
def delete_notification(id: int, db: Session = Depends(get_db)):
    db_notif = db.query(NotificationDB).filter(NotificationDB.id == id).first()
    if not db_notif:
        raise HTTPException(status_code=404, detail="Notificación no encontrada")
    db.delete(db_notif)
    db.commit()
    return {"message": "Notificación eliminada"}

@app.get("/subjects/{sub_id}/participants", response_model=List[User])
def get_subject_participants(sub_id: int, db: Session = Depends(get_db)):
    subject = db.query(SubjectDB).filter(SubjectDB.id == sub_id).first()
    if not subject:
        raise HTTPException(status_code=404, detail="Materia no encontrada")

    participants = []

    # 1. Add Professors assigned to this subject in any schedule
    professors = db.query(UserDB).join(FacilityScheduleDB, UserDB.id == FacilityScheduleDB.professorId).filter(FacilityScheduleDB.subjectId == sub_id).distinct().all()
    participants.extend(professors)

    # 2. Add Enrolled Students
    students = db.query(UserDB).join(EnrollmentDB, UserDB.id == EnrollmentDB.studentId).filter(EnrollmentDB.subjectId == sub_id).all()
    for s in students:
        if s not in participants:
            participants.append(s)

    return participants

def get_local_ip():
    try:
        import socket
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

if __name__ == "__main__":
    import uvicorn

    def start_tunnel():
        import os
        import time
        time.sleep(2)
        tunnel_script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "run_tunnel_and_update_app.py")
        if os.path.exists(tunnel_script):
            try:
                import run_tunnel_and_update_app
                run_tunnel_and_update_app.main()
            except Exception as e:
                print(f"[!] Error al iniciar Cloudflare Tunnel: {e}")

    tunnel_thread = threading.Thread(target=start_tunnel, daemon=True)
    tunnel_thread.start()

    local_ip = get_local_ip()
    print("===================================================")
    print("    AULA VIRTUAL - SERVIDOR FASTAPI BACKEND")
    print("===================================================")
    print(f"Servidor FastAPI ejecutándose en el puerto 8000:")
    print(f"  • PC Localhost:            http://localhost:8000/")
    print(f"  • Emulador Android Studio: http://10.0.2.2:8000/")
    print(f"  • Red Local Wi-Fi:         http://{local_ip}:8000/")
    print("===================================================\n")

    uvicorn.run(app, host="0.0.0.0", port=8000)
