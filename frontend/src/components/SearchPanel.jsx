import React, { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  IconButton,
  Stack,
  Collapse,
  Divider,
  Chip,
  CircularProgress
} from '@mui/material';

const STRING_OPERATORS = [
  { value: 'Contains', label: 'Contains' },
  { value: 'StartsWith', label: 'Starts With' },
  { value: 'Equals', label: 'Equals' }
];

const NUMERIC_OPERATORS = [
  { value: 'Equals', label: '= Equals' },
  { value: 'GreaterThan', label: '> Greater Than' },
  { value: 'GreaterThanOrEqual', label: '>= Greater or Equal' },
  { value: 'LessThan', label: '< Less Than' },
  { value: 'LessThanOrEqual', label: '<= Less or Equal' },
  { value: 'Between', label: 'Between' }
];

const DATE_OPERATORS = [
  { value: 'Between', label: 'Date Range (From - To)' },
  { value: 'Equals', label: '= Exact Date' },
  { value: 'GreaterThanOrEqual', label: '>= On or After' },
  { value: 'LessThanOrEqual', label: '<= On or Before' }
];

function getOperatorsForType(type) {
  const t = (type || '').toLowerCase();
  if (t === 'date') return DATE_OPERATORS;
  if (t === 'decimal' || t === 'integer') return NUMERIC_OPERATORS;
  return STRING_OPERATORS;
}

function isNumericType(type) {
  const t = (type || '').toLowerCase();
  return t === 'decimal' || t === 'integer';
}

function isDateType(type) {
  const t = (type || '').toLowerCase();
  return t === 'date';
}

/**
 * SearchPanel renders a dynamic search form based on the table schema.
 *
 * Props:
 * - columns: Array of { name, type } from the table schema
 * - onSearch: (filters) => void — called with array of filter objects
 * - onClear: () => void — called to reset search results
 * - loading: boolean
 */
export default function SearchPanel({ columns, onSearch, onClear, loading }) {
  const [expanded, setExpanded] = useState(false);

  const selectableColumns = [...columns, { name: 'DATE', type: 'date' }];
  const todayStr = new Date().toISOString().split('T')[0];

  const [filters, setFilters] = useState([createEmptyFilter()]);

  function createEmptyFilter() {
    const firstCol = selectableColumns.length > 0 ? selectableColumns[0] : null;
    return {
      column: firstCol ? firstCol.name : '',
      operator: firstCol ? getOperatorsForType(firstCol.type)[0].value : 'Contains',
      value: '',
      maxValue: ''
    };
  }

  const handleAddFilter = () => {
    setFilters([...filters, createEmptyFilter()]);
  };

  const handleRemoveFilter = (index) => {
    if (filters.length <= 1) return;
    setFilters(filters.filter((_, i) => i !== index));
  };

  const handleFilterChange = (index, field, value) => {
    const updated = [...filters];
    let valToSet = value;

    const targetColName = field === 'column' ? value : updated[index].column;
    const colDef = selectableColumns.find(c => c.name === targetColName);

    if (colDef && isDateType(colDef.type) && (field === 'value' || field === 'maxValue')) {
      if (valToSet && valToSet > todayStr) {
        valToSet = todayStr;
      }
    }

    updated[index] = { ...updated[index], [field]: valToSet };

    // When column changes, reset operator to first valid one
    if (field === 'column') {
      const ops = getOperatorsForType(colDef?.type);
      updated[index].operator = ops[0].value;
      updated[index].value = '';
      updated[index].maxValue = '';
    }

    // When operator changes, reset maxValue if not Between
    if (field === 'operator' && value !== 'Between') {
      updated[index].maxValue = '';
    }

    setFilters(updated);
  };

  const handleSearch = () => {
    // Build filter payloads, skipping empty values
    const validFilters = filters
      .filter(f => f.column && f.value !== '')
      .map(f => {
        const colDef = selectableColumns.find(c => c.name === f.column);
        const numeric = isNumericType(colDef?.type);
        const isDate = isDateType(colDef?.type);

        let val = f.value;
        if (numeric) val = parseFloat(f.value);
        if (isDate && val > todayStr) val = todayStr;

        const payload = {
          column: f.column,
          operator: f.operator,
          value: val
        };
        if (f.operator === 'Between' && f.maxValue !== '') {
          let maxVal = f.maxValue;
          if (numeric) maxVal = parseFloat(f.maxValue);
          if (isDate && maxVal > todayStr) maxVal = todayStr;
          payload.maxValue = maxVal;
        }
        return payload;
      });

    if (validFilters.length === 0) return;
    onSearch(validFilters);
  };

  const handleClear = () => {
    setFilters([createEmptyFilter()]);
    onClear();
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <Paper sx={{ mb: 3, boxShadow: 'none', border: '1px solid #e0e0e0', overflow: 'hidden' }}>
      {/* Header toggle */}
      <Box
        onClick={() => setExpanded(!expanded)}
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          px: 3,
          py: 1.5,
          cursor: 'pointer',
          bgcolor: '#fafafa',
          '&:hover': { bgcolor: '#f5f5f5' }
        }}
      >
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography variant="body2" color="primary" sx={{ fontSize: '1.1rem' }}>🔍</Typography>
          <Typography variant="subtitle1" sx={{ fontWeight: 550 }}>
            Search & Filter Records
          </Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary">
          {expanded ? '▲' : '▼'}
        </Typography>
      </Box>

      <Collapse in={expanded}>
        <Divider />
        <Box sx={{ p: 3 }}>
          <Stack spacing={2}>
            {filters.map((filter, index) => {
              const colDef = selectableColumns.find(c => c.name === filter.column);
              const operators = getOperatorsForType(colDef?.type);
              const isBetween = filter.operator === 'Between';
              const numeric = isNumericType(colDef?.type);
              const isDate = isDateType(colDef?.type);

              return (
                <Stack key={index} direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'stretch', sm: 'flex-start' }}>
                  {/* Column selector */}
                  <FormControl size="small" sx={{ minWidth: { xs: '100%', sm: 180 } }}>
                    <InputLabel>Column</InputLabel>
                    <Select
                      value={filter.column}
                      onChange={(e) => handleFilterChange(index, 'column', e.target.value)}
                      label="Column"
                    >
                      {selectableColumns.map((col, i) => (
                        <MenuItem key={i} value={col.name}>
                          <Stack direction="row" spacing={1} alignItems="center">
                            <span>{col.name}</span>
                            <Chip
                              label={col.type}
                              size="small"
                              variant="outlined"
                              sx={{ height: 20, fontSize: '0.7rem' }}
                            />
                          </Stack>
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  {/* Operator selector */}
                  <FormControl size="small" sx={{ minWidth: { xs: '100%', sm: 170 } }}>
                    <InputLabel>Operator</InputLabel>
                    <Select
                      value={filter.operator}
                      onChange={(e) => handleFilterChange(index, 'operator', e.target.value)}
                      label="Operator"
                    >
                      {operators.map((op) => (
                        <MenuItem key={op.value} value={op.value}>
                          {op.label}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  {/* Value input */}
                  <TextField
                    size="small"
                    label={isBetween ? (isDate ? 'From Date' : 'Min Value') : (isDate ? 'Date' : 'Value')}
                    type={numeric ? 'number' : (isDate ? 'date' : 'text')}
                    value={filter.value}
                    onChange={(e) => handleFilterChange(index, 'value', e.target.value)}
                    onKeyDown={handleKeyDown}
                    InputLabelProps={isDate ? { shrink: true } : undefined}
                    inputProps={isDate ? { max: todayStr } : undefined}
                    sx={{ minWidth: 160, flex: 1 }}
                  />

                  {/* Max value input for Between */}
                  {isBetween && (
                    <TextField
                      size="small"
                      label={isDate ? 'To Date' : 'Max Value'}
                      type={numeric ? 'number' : (isDate ? 'date' : 'text')}
                      value={filter.maxValue}
                      onChange={(e) => handleFilterChange(index, 'maxValue', e.target.value)}
                      onKeyDown={handleKeyDown}
                      InputLabelProps={isDate ? { shrink: true } : undefined}
                      inputProps={isDate ? { max: todayStr } : undefined}
                      sx={{ minWidth: 160, flex: 1 }}
                    />
                  )}

                  {/* Remove filter button */}
                  <IconButton
                    size="small"
                    onClick={() => handleRemoveFilter(index)}
                    disabled={filters.length <= 1}
                    sx={{ mt: 0.5, color: 'error.main' }}
                    aria-label="Remove filter"
                  >
                    ✕
                  </IconButton>
                </Stack>
              );
            })}
          </Stack>

          <Divider sx={{ my: 2 }} />

          {/* Action buttons */}
          <Stack direction="row" spacing={2} justifyContent="space-between">
            <Button
              variant="text"
              size="small"
              onClick={handleAddFilter}
              sx={{ textTransform: 'none' }}
            >
              + Add Filter
            </Button>

            <Stack direction="row" spacing={1}>
              <Button
                variant="outlined"
                size="small"
                onClick={handleClear}
                disabled={loading}
                sx={{ textTransform: 'none' }}
              >
                Clear
              </Button>
              <Button
                variant="contained"
                size="small"
                onClick={handleSearch}
                disabled={loading}
                sx={{ textTransform: 'none' }}
              >
                {loading ? <CircularProgress size={16} color="inherit" /> : 'Search'}
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Collapse>
    </Paper>
  );
}
