import os
import sys

if __name__ == "__main__":
    backend_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(backend_dir)
    if backend_dir not in sys.path:
        sys.path.insert(0, backend_dir)

    print("===================================================")
    print("    AULA VIRTUAL - SERVIDOR BACKEND FASTAPI (PYTHON)")
    print("===================================================")
    print("Iniciando servidor Uvicorn en http://localhost:8000 ...\n")

    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
