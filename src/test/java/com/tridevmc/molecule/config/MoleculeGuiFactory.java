package com.tridevmc.molecule.config;

import com.tridevmc.compound.config.CompoundGuiFactory;
import com.tridevmc.molecule.Molecule;

public class MoleculeGuiFactory extends CompoundGuiFactory {
    public MoleculeGuiFactory() {
        super(Molecule.CONFIG);
    }
}
