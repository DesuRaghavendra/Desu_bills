import React, { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { getTables, deleteTable } from '../services/tableService';
import {
  Container,
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  TextField,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions
} from '@mui/material';
import { useNavigate } from 'react-router-dom';

export default function Dashboard() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [tables, setTables] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedTable, setSelectedTable] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const fetchTables = async () => {
    try {
      setLoading(true);
      const data = await getTables();
      setTables(data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to retrieve tables');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTables();
  }, []);

  const handleDeleteClick = (table) => {
    setSelectedTable(table);
    setOpenDialog(true);
  };

  const handleConfirmDelete = async () => {
    if (!selectedTable) return;
    setDeleteLoading(true);
    try {
      await deleteTable(selectedTable.tableId);
      setTables(tables.filter((t) => t.tableId !== selectedTable.tableId));
      setOpenDialog(false);
      setSelectedTable(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete table');
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleCreateClick = () => {
    navigate('/create-table');
  };

  const filteredTables = tables.filter((t) =>
    t.tableName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const formatDate = (isoString) => {
    if (!isoString) return '-';
    const date = new Date(isoString);
    return date.toLocaleString();
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' }, gap: 2, mb: 4, pb: 2, borderBottom: '1px solid #e0e0e0' }}>
        <Typography variant="h5" sx={{ fontWeight: 600, color: '#333' }}>
          OCR-Table Workspace
        </Typography>
        <Button
          variant="outlined"
          color="error"
          size="small"
          onClick={logout}
          sx={{ textTransform: 'none', boxShadow: 'none' }}
        >
          Sign Out
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: { xs: 'stretch', sm: 'center' }, gap: 2, mb: 3 }}>
        <TextField
          placeholder="Search by table name..."
          variant="outlined"
          size="small"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          sx={{ width: { xs: '100%', sm: 300 } }}
        />
        <Button
          variant="contained"
          onClick={handleCreateClick}
          sx={{
            bgcolor: '#1976d2',
            '&:hover': { bgcolor: '#1565c0' },
            boxShadow: 'none',
            textTransform: 'none'
          }}
        >
          Create Table
        </Button>
      </Box>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress size={30} />
        </Box>
      ) : (
        <TableContainer component={Paper} variant="outlined" sx={{ boxShadow: 'none', borderColor: '#e0e0e0' }}>
          <Table sx={{ minWidth: 650 }} aria-label="custom logical tables grid">
            <TableHead sx={{ bgcolor: '#f5f5f5' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 600 }}>Table Name</TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>Total Records</TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>Created Date</TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredTables.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No dynamic tables found. Click "Create Table" to upload a new one.
                  </TableCell>
                </TableRow>
              ) : (
                filteredTables.map((row) => (
                  <TableRow
                    key={row.tableId}
                    sx={{
                      '&:last-child td, &:last-child th': { border: 0 },
                      '&:hover': { bgcolor: '#fafafa', cursor: 'pointer' }
                    }}
                    onClick={() => navigate(`/tables/${row.tableId}`)}
                  >
                    <TableCell component="th" scope="row" sx={{ fontWeight: 550, color: '#1976d2' }}>
                      {row.tableName}
                    </TableCell>
                    <TableCell align="right">{row.totalRecords}</TableCell>
                    <TableCell align="right">{formatDate(row.createdAt)}</TableCell>
                    <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                      <Button
                        size="small"
                        color="error"
                        onClick={() => handleDeleteClick(row)}
                        sx={{ textTransform: 'none' }}
                      >
                        Delete
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog
        open={openDialog}
        onClose={() => setOpenDialog(false)}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
        PaperProps={{
          variant: 'outlined',
          sx: { boxShadow: 'none', borderColor: '#e0e0e0', p: 1 }
        }}
      >
        <DialogTitle id="alert-dialog-title" sx={{ fontWeight: 600 }}>
          {"Confirm Table Deletion?"}
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            Are you sure you want to permanently delete the table <strong>{selectedTable?.tableName}</strong> and all its associated records? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)} disabled={deleteLoading} sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button onClick={handleConfirmDelete} disabled={deleteLoading} color="error" autoFocus sx={{ textTransform: 'none' }}>
            {deleteLoading ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
